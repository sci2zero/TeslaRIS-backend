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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.exceptionhandling.exception.RegistryBookException;
import rs.teslaris.core.util.exceptionhandling.exception.StorageException;
import rs.teslaris.core.util.files.ResourceMultipartFile;
import rs.teslaris.core.util.language.SerbianTransliteration;
import rs.teslaris.thesislibrary.model.RegistryBookEntry;
import rs.teslaris.thesislibrary.model.RegistryBookReport;
import rs.teslaris.thesislibrary.repository.RegistryBookEntryRepository;
import rs.teslaris.thesislibrary.repository.RegistryBookReportRepository;
import rs.teslaris.thesislibrary.service.interfaces.RegistryBookReportService;
import rs.teslaris.thesislibrary.util.RegistryBookGenerationUtil;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
@Traceable
public class RegistryBookReportServiceImpl implements RegistryBookReportService {

    private final RegistryBookEntryRepository registryBookEntryRepository;

    private final FileService fileService;

    private final RegistryBookReportRepository registryBookReportRepository;

    private final OrganisationUnitService organisationUnitService;

    private final UserRepository userRepository;

    private final TaskManagerService taskManagerService;


    @Override
    public String scheduleReportGeneration(RelativeDateDTO from, RelativeDateDTO to,
                                           Integer institutionId,
                                           String lang, Integer userId, String authorName,
                                           String authorTitle, RecurrenceType recurrence) {
        var dateFrom = from.computeDate();
        var dateTo = to.computeDate();

        if (dateFrom.isAfter(dateTo)) {
            throw new RegistryBookException("dateRangeIssueMessage");
        }

        var reportGenerationTime = taskManagerService.findNextFreeExecutionTime();
        var taskId = taskManagerService.scheduleTask(
            "Registry_Book-" + institutionId +
                "-" + from + "_" + to + "_" + lang +
                "-" + UUID.randomUUID(), reportGenerationTime,
            () -> generateReport(dateFrom, dateTo, authorName, authorTitle, institutionId, lang),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, reportGenerationTime,
                ScheduledTaskType.REGISTRY_BOOK_REPORT_GENERATION, new HashMap<>() {{
                put("institutionId", institutionId);
                put("from", from);
                put("to", to);
                put("authorTitle", authorTitle);
                put("userId", userId);
                put("lang", lang);
                put("authorName", authorName);
            }}, recurrence));

        return reportGenerationTime.getHour() + ":" + reportGenerationTime.getMinute() + "h";
    }

    private void generateReport(LocalDate from, LocalDate to, String authorName,
                                String authorTitle, Integer institutionId, String lang) {
        Map<String, List<List<String>>> groupedReportTableRows =
            loadGroupedRegistryBookRows(from, to, authorName, authorTitle, institutionId, lang);

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
            .orElseThrow(() -> new StorageException("No report with given filename."));
        var userInstitution = userRepository.findOrganisationUnitIdForUser(userId);

        if (Objects.nonNull(userInstitution) && userInstitution > 0 &&
            !organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(userInstitution)
                .contains(report.getInstitution().getId())) {
            throw new LoadingException("Unauthorised to view report.");
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
                                                                        String authorName,
                                                                        String authorTitle,
                                                                        Integer institutionId,
                                                                        String lang) {
        int pageNumber = 0;
        int chunkSize = 100;
        boolean hasNextPage = true;
        var groupedRows = new TreeMap<String, List<List<String>>>();

        while (hasNextPage) {
            List<RegistryBookEntry> chunk =
                registryBookEntryRepository.getRegistryBookEntriesForInstitutionAndPeriod(
                        List.of(institutionId), from, to, authorName, authorTitle,
                        SerbianTransliteration.toCyrillic(authorName),
                        SerbianTransliteration.toCyrillic(authorTitle),
                        PageRequest.of(pageNumber, chunkSize))
                    .getContent();

            RegistryBookGenerationUtil.constructRowsForChunk(groupedRows, chunk, lang);

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
                RegistryBookGenerationUtil.getTableLabel("reporting.registry-book.school-year",
                    lang) + " " + schoolYear,
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

    private List<String> getHeaderColumns(String lang) {
        return List.of(
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.number", lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.name", lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.birth-info",
                lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.parents",
                lang),
            RegistryBookGenerationUtil.getTableLabel(
                "reporting.registry-book-column.previous-institution", lang),
            RegistryBookGenerationUtil.getTableLabel(
                "reporting.registry-book-column.previous-title", lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.institution",
                lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.thesis", lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.commission",
                lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.defence",
                lang),
            RegistryBookGenerationUtil.getTableLabel("reporting.registry-book-column.title", lang),
            RegistryBookGenerationUtil.getTableLabel(
                "reporting.registry-book-column.diploma-number", lang),
            RegistryBookGenerationUtil.getTableLabel(
                "reporting.registry-book-column.promotion-date", lang)
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
        return RegistryBookGenerationUtil.getTransliteratedContent(institution.getName())
            .replace(" ", "_") +
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
