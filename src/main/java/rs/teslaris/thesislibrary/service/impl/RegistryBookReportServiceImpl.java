package rs.teslaris.thesislibrary.service.impl;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import io.minio.GetObjectResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.util.ResourceMultipartFile;
import rs.teslaris.core.util.exceptionhandling.exception.RegistryBookException;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.thesislibrary.model.DissertationInformation;
import rs.teslaris.thesislibrary.model.PreviousTitleInformation;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;
import rs.teslaris.thesislibrary.model.RegistryBookPersonalInformation;
import rs.teslaris.thesislibrary.model.RegistryBookReport;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryRepository;
import rs.teslaris.thesislibrary.repository.RegistryBookReportRepository;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RegistryBookReportServiceImpl implements RegistryBookReportService {

    private final RegistryBookEntryRepository registryBookEntryRepository;

    private final MessageSource messageSource;

    private final FileService fileService;

    private final RegistryBookReportRepository registryBookReportRepository;

    private final OrganisationUnitService organisationUnitService;

    private final UserRepository userRepository;

    private final TaskManagerService taskManagerService;

    private final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.GERMANY);


    @Override
    public String scheduleReportGeneration(LocalDate from, LocalDate to, Integer institutionId,
                                           String lang, Integer userId) {
        var reportGenerationTime = taskManagerService.findNextFreeExecutionTime();
        taskManagerService.scheduleTask(
            "Registry_Book-" + institutionId +
                "-" + from + "_" + to + "_" + lang +
                "-" + UUID.randomUUID(), reportGenerationTime,
            () -> generateReport(from, to, institutionId, lang),
            userId);
        return reportGenerationTime.getHour() + ":" + reportGenerationTime.getMinute() + "h";
    }

    private void generateReport(LocalDate from, LocalDate to, Integer institutionId,
                                String lang) {
        Map<String, List<List<String>>> groupedReportTableRows =
            loadGroupedRegistryBookRows(from, to, institutionId, lang);

        try (var out = new ByteArrayOutputStream()) {
            var document = new Document(PageSize.A1, 36, 36, 54, 36);
            var writer = PdfWriter.getInstance(document, out);

            var baseFont = loadBaseFont();
            var headingFont = new Font(baseFont, 18, Font.BOLD);
            var headerFont = new Font(baseFont, 10, Font.BOLD);
            var cellFont = new Font(baseFont, 10, Font.NORMAL);

            List<String> headerColumns = getHeaderColumns(lang);
            writer.setPageEvent(new TableHeaderEvent(headerFont, headerColumns));

            document.open();

            // Initial spacer, to ensure that document is not empty even with no results
            document.add(new Chunk(""));

            addReportSections(document, groupedReportTableRows, headingFont, cellFont, lang);

            document.close();
            writer.close();

            var reportName = generateReportFileName(from, to, institutionId);
            var serverFilename =
                fileService.store(convertToMultipartFile(out.toByteArray(), reportName),
                    reportName.split("\\.")[0]);

            registryBookReportRepository.findByReportFileName(serverFilename)
                .ifPresent(registryBookReportRepository::delete);
            var reportFile = new RegistryBookReport();
            reportFile.setInstitution(organisationUnitService.findOne(institutionId));
            reportFile.setReportFileName(serverFilename);
            registryBookReportRepository.save(reportFile);
        } catch (Exception e) {
            log.error("PDF generation failed. Reason: {}", e.getMessage());
            throw new RegistryBookException("PDF generation failed.");
        }
    }

    @Override
    public List<String> listAvailableReports(Integer userId) {
        var availableReports = new ArrayList<String>();
        var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.nonNull(userInstitution) && userInstitution > 0) {
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                userInstitution).forEach(institutionId -> {
                availableReports.addAll(
                    registryBookReportRepository.findForInstitution(institutionId).stream()
                        .map(RegistryBookReport::getReportFileName).toList());
            });
        } else {
            availableReports.addAll(registryBookReportRepository.findAll().stream()
                .map(RegistryBookReport::getReportFileName).toList());
        }

        return availableReports;
    }

    @Override
    public GetObjectResponse serveReportFile(String reportFileName, Integer userId)
        throws IOException {
        var report = registryBookReportRepository.findByReportFileName(reportFileName)
            .orElseThrow(() -> new RegistryBookException("No report with given filename."));
        var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.nonNull(userInstitution) && userInstitution > 0 &&
            !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitution)
                .contains(report.getInstitution().getId())) {
            throw new RegistryBookException("Unauthorised to view report.");
        }

        return fileService.loadAsResource(reportFileName);
    }

    @Override
    public void deleteReportFile(String reportFileName, Integer userId) {
        registryBookReportRepository.findByReportFileName(reportFileName)
            .ifPresent(report -> {
                var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

                if (Objects.nonNull(userInstitution) && userInstitution > 0 &&
                    !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitution)
                        .contains(report.getInstitution().getId())) {
                    throw new RegistryBookException("Unauthorised to delete report.");
                }

                registryBookReportRepository.delete(report);
            });
    }

    private Map<String, List<List<String>>> loadGroupedRegistryBookRows(LocalDate from,
                                                                        LocalDate to,
                                                                        Integer institutionId,
                                                                        String lang) {
        int pageNumber = 0;
        int chunkSize = 10;
        boolean hasNextPage = true;
        var groupedRows = new TreeMap<String, List<List<String>>>();

        while (hasNextPage) {
            List<RegistryBookEntry> chunk = registryBookEntryRepository
                .getRegistryBookCountForInstitutionAndPeriod(institutionId, from, to,
                    PageRequest.of(pageNumber, chunkSize))
                .getContent();

            constructRowsForChunk(groupedRows, chunk, lang);

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }

        return groupedRows;
    }

    private BaseFont loadBaseFont() throws DocumentException, IOException {
        return BaseFont.createFont("src/main/resources/fonts/DejaVuSans.ttf", BaseFont.IDENTITY_H,
            BaseFont.EMBEDDED);
    }

    private void addReportSections(Document document,
                                   Map<String, List<List<String>>> groupedRows,
                                   Font headingFont,
                                   Font cellFont,
                                   String lang) throws DocumentException {
        for (var entry : groupedRows.entrySet()) {
            String schoolYear = entry.getKey();
            List<List<String>> rows = entry.getValue();

            var sectionTitle = new Paragraph(
                getTableLabel("reporting.registry-book.school-year", lang) + " " + schoolYear,
                headingFont);
            sectionTitle.setAlignment(Element.ALIGN_CENTER);
            sectionTitle.setSpacingBefore(10f);
            sectionTitle.setSpacingAfter(10f);

            document.add(sectionTitle);

            if (!rows.isEmpty()) {
                var table = constructReportTable(rows, cellFont);
                document.add(table);
            }
        }
    }

    @NotNull
    private PdfPTable constructReportTable(List<List<String>> rows, Font cellFont) {
        var columnCount = rows.getFirst().size();
        var table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(20f);


        for (List<String> row : rows) {
            var index = 0;
            for (var cellValue : row) {
                var cell = new PdfPCell(new Phrase(cellValue, cellFont));

                if (index == 0) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                }

                cell.setPadding(4);
                cell.setUseAscender(true);
                cell.setUseDescender(true);
                table.addCell(cell);
                index++;
            }
        }
        return table;
    }

    private void constructRowsForChunk(TreeMap<String, List<List<String>>> rows,
                                       List<RegistryBookEntry> chunk, String lang) {
        for (RegistryBookEntry entry : chunk) {
            var rowData = new ArrayList<String>();

            addBookNumbers(rowData, entry);
            addAuthorInfo(rowData, entry.getPersonalInformation(), lang);
            addPreviousInstitutionInfo(rowData, entry.getPreviousTitleInformation());
            addPreviousTitleInfo(rowData, entry.getPreviousTitleInformation());
            addDissertationInstitution(rowData, entry.getDissertationInformation());
            addDissertationTitle(rowData, entry.getDissertationInformation(), lang);
            addCommissionAndMentor(rowData, entry.getDissertationInformation());
            addDefenceInfo(rowData, entry.getDissertationInformation(), lang);
            rowData.add(entry.getDissertationInformation().getAcquiredTitle());
            setDiplomaInformation(rowData, entry.getDissertationInformation(), lang);

            rowData.add(entry.getPromotion().getPromotionDate().format(DATE_FORMATTER));

            rows.computeIfAbsent(entry.getPromotionSchoolYear(), k -> new ArrayList<>())
                .add(rowData);
        }
    }

    private void addBookNumbers(List<String> rowData, RegistryBookEntry entry) {
        rowData.add(
            entry.getRegistryBookNumber() + "\n---------\n" + entry.getPromotionOrdinalNumber());
    }

    private void addAuthorInfo(ArrayList<String> rowData, RegistryBookPersonalInformation info,
                               String lang) {
        rowData.add(info.getAuthorName().toString());
        setAuthorBirthInformation(rowData, info, lang);
        setAuthorParentInformation(rowData, info, lang);
    }

    private void addPreviousInstitutionInfo(List<String> rowData, PreviousTitleInformation info) {
        rowData.add(info.getInstitutionName() + ", " + info.getInstitutionPlace());
    }

    private void addPreviousTitleInfo(List<String> rowData, PreviousTitleInformation info) {
        rowData.add(info.getAcademicTitle().getValue() + "\n" + info.getSchoolYear());
    }

    private void addDissertationInstitution(List<String> rowData, DissertationInformation info) {
        rowData.add(getTransliteratedContent(info.getOrganisationUnit().getName()) +
            ", " + info.getInstitutionPlace());
    }

    private void addDissertationTitle(List<String> rowData, DissertationInformation info,
                                      String lang) {
        rowData.add(getTableLabel("reporting.registry-book.dissertation", lang) + "\n" +
            info.getDissertationTitle());
    }

    private void addCommissionAndMentor(List<String> rowData, DissertationInformation info) {
        String commission = info.getCommission();
        String mentor = info.getMentor();
        rowData.add(commission + "\n" + (commission.contains(mentor) ? "" : mentor));
    }

    private void addDefenceInfo(List<String> rowData, DissertationInformation info, String lang) {
        StringBuilder defenceInfo = new StringBuilder();

        if (!info.getGrade().isBlank()) {
            defenceInfo.append(getTableLabel("reporting.registry-book.grade", lang))
                .append("\n").append(info.getGrade()).append("\n");
        }

        defenceInfo.append(getTableLabel("reporting.registry-book.defended", lang))
            .append("\n").append(info.getDefenceDate().format(DATE_FORMATTER));

        rowData.add(defenceInfo.toString());
    }

    private void setAuthorBirthInformation(ArrayList<String> rowData,
                                           RegistryBookPersonalInformation personalInformation,
                                           String lang) {
        var birthInformation = getTableLabel("reporting.registry-book.date", lang) + "\n" +
            personalInformation.getLocalBirthDate().format(DATE_FORMATTER) + "\n" +
            getTableLabel("reporting.registry-book.place", lang) + "\n" +
            personalInformation.getPlaceOfBrith() + "\n" +
            getTableLabel("reporting.registry-book.municipality", lang) +
            "\n" +
            personalInformation.getMunicipalityOfBrith() +
            "\n" +
            getTableLabel("reporting.registry-book.country", lang) +
            "\n" +
            getTransliteratedContent(
                personalInformation.getCountryOfBirth().getName()) +
            "\n";

        rowData.add(birthInformation);
    }

    private void setAuthorParentInformation(ArrayList<String> rowData,
                                            RegistryBookPersonalInformation personalInformation,
                                            String lang) {
        var parentInformation = new StringBuilder();

        if (!personalInformation.getFatherName().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.father", lang))
                .append("\n");
            parentInformation.append(personalInformation.getFatherName()).append(" ")
                .append(personalInformation.getFatherSurname()).append("\n");
        }

        if (!personalInformation.getMotherName().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.mother", lang))
                .append("\n");
            parentInformation.append(personalInformation.getMotherName()).append(" ")
                .append(personalInformation.getMotherSurname()).append("\n");
        }

        if (!personalInformation.getGuardianNameAndSurname().isBlank()) {
            parentInformation.append(getTableLabel("reporting.registry-book.guardian", lang))
                .append("\n");
            parentInformation.append(personalInformation.getGuardianNameAndSurname()).append("\n");
        }

        rowData.add(parentInformation.toString());
    }

    private void setDiplomaInformation(ArrayList<String> rowData,
                                       DissertationInformation dissertationInformation,
                                       String lang) {
        var diplomaInformation = new StringBuilder();

        if (!dissertationInformation.getDiplomaNumber().isBlank()) {
            diplomaInformation.append(getTableLabel("reporting.registry-book.diploma-number", lang))
                .append("\n");
            diplomaInformation.append(dissertationInformation.getDiplomaNumber()).append("\n")
                .append(getTableLabel("reporting.registry-book.date", lang)).append("\n")
                .append(dissertationInformation.getDiplomaIssueDate().format(DATE_FORMATTER))
                .append("\n");
        }

        if (!dissertationInformation.getDiplomaSupplementsNumber().isBlank()) {
            diplomaInformation.append(
                    getTableLabel("reporting.registry-book.supplements-number", lang))
                .append("\n");
            diplomaInformation.append(dissertationInformation.getDiplomaSupplementsNumber())
                .append("\n")
                .append(getTableLabel("reporting.registry-book.date", lang)).append("\n")
                .append(
                    dissertationInformation.getDiplomaSupplementsIssueDate().format(DATE_FORMATTER))
                .append("\n");
        }

        rowData.add(diplomaInformation.toString());
    }

    private String getTableLabel(String fieldName, String lang) {
        return messageSource.getMessage(
            fieldName,
            new Object[] {},
            Locale.forLanguageTag(lang)
        );
    }

    private String getTransliteratedContent(Set<MultiLingualContent> multilingualContent) {
        MultiLingualContent fallback = null;
        for (MultiLingualContent content : multilingualContent) {
            if ("SR".equalsIgnoreCase(content.getLanguage().getLanguageTag())) {
                return SerbianTransliteration.toCyrillic(content.getContent());
            }
            fallback = content;
        }
        return fallback != null ? fallback.getContent() : "";
    }

    private List<String> getHeaderColumns(String lang) {
        return List.of(
            getTableLabel("reporting.registry-book-column.number", lang),
            getTableLabel("reporting.registry-book-column.name", lang),
            getTableLabel("reporting.registry-book-column.birth-info", lang),
            getTableLabel("reporting.registry-book-column.parents", lang),
            getTableLabel("reporting.registry-book-column.previous-institution", lang),
            getTableLabel("reporting.registry-book-column.previous-title", lang),
            getTableLabel("reporting.registry-book-column.institution", lang),
            getTableLabel("reporting.registry-book-column.thesis", lang),
            getTableLabel("reporting.registry-book-column.commission", lang),
            getTableLabel("reporting.registry-book-column.defence", lang),
            getTableLabel("reporting.registry-book-column.title", lang),
            getTableLabel("reporting.registry-book-column.diploma-number", lang),
            getTableLabel("reporting.registry-book-column.promotion-date", lang)
        );
    }

    private MultipartFile convertToMultipartFile(byte[] document, String fileName)
        throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(document);
        return new ResourceMultipartFile(fileName, fileName,
            "application/pdf",
            new ByteArrayResource(byteArrayOutputStream.toByteArray()));
    }

    private String generateReportFileName(LocalDate from, LocalDate to, Integer institutionId) {
        var institution = organisationUnitService.findOne(institutionId);
        return getTransliteratedContent(institution.getName()).replace(" ", "_") +
            "_" + from + "_" + to + ".pdf";
    }

    @Scheduled(cron = "0 0 0 * * ?") // Every day at 00:00 AM
    public void deleteOldGeneratedReports() {
        var oneYearAgo = LocalDate.now().minusYears(1);
        registryBookReportRepository.findStaleReports(
                Date.from(oneYearAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .forEach(registryBookReport -> {
                fileService.delete(registryBookReport.getReportFileName());
                registryBookReportRepository.delete(registryBookReport);
            });
    }

    private static class TableHeaderEvent extends PdfPageEventHelper {
        private final Font headerFont;
        private final List<String> headerColumns;
        private final int columnCount;

        public TableHeaderEvent(Font headerFont, List<String> headerColumns) {
            this.headerFont = headerFont;
            this.headerColumns = headerColumns;
            this.columnCount = headerColumns.size();
        }

        @Override
        public void onStartPage(PdfWriter writer, Document document) {
            var table = new PdfPTable(columnCount);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(20f);


            for (int i = 0; i < columnCount; i++) {
                var headerCell = new PdfPCell(new Phrase(headerColumns.get(i), headerFont));
                headerCell.setBackgroundColor(new BaseColor(224, 255, 255));
                headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                headerCell.setPadding(5);
                table.addCell(headerCell);
            }

            try {
                document.add(table);
            } catch (DocumentException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
