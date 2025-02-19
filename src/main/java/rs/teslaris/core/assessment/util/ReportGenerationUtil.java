package rs.teslaris.core.assessment.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.service.interfaces.document.FileService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.ResourceMultipartFile;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.language.SerbianTransliteration;

@Component
public class ReportGenerationUtil {

    private static MessageSource messageSource;

    private static FileService fileService;

    private static OrganisationUnitIndexRepository organisationUnitIndexRepository;


    @Autowired
    public ReportGenerationUtil(MessageSource messageSource, FileService fileService,
                                OrganisationUnitIndexRepository organisationUnitIndexRepository) {
        ReportGenerationUtil.messageSource = messageSource;
        ReportGenerationUtil.fileService = fileService;
        ReportGenerationUtil.organisationUnitIndexRepository = organisationUnitIndexRepository;
    }

    public static XWPFDocument loadDocumentTemplate(String templateName) throws IOException {
        FileInputStream fis =
            new FileInputStream("src/main/resources/reportTemplates/" + templateName);
        var document = new XWPFDocument(fis);
        fis.close();
        return document;
    }

    public static void dynamicallyGenerateTable(XWPFDocument document, List<List<String>> tableData,
                                                Integer tableIndex) {
        var table = document.getTables().get(tableIndex);

        if (table.getRows().size() > 1) {
            table.removeRow(1);
        }

        for (List<String> rowData : tableData) {
            var row = table.createRow();

            for (int i = 0; i < rowData.size(); i++) {
                var cell = row.getCell(i) != null ? row.getCell(i) : row.createCell();
                cell.setText(rowData.get(i));
            }
        }
    }

    public static void insertFields(XWPFDocument document, Map<String, String> replacements) {
        document.getParagraphs().forEach(paragraph -> processParagraph(paragraph, replacements));

        document.getTables().forEach(table ->
            table.getRows().forEach(row ->
                row.getTableCells().forEach(cell ->
                    cell.getParagraphs().forEach(paragraph ->
                        processParagraph(paragraph, replacements)
                    )
                )
            )
        );
    }

    private static void processParagraph(XWPFParagraph paragraph,
                                         Map<String, String> replacements) {
        StringBuilder fullText = new StringBuilder();
        for (var run : paragraph.getRuns()) {
            if (run.getText(0) != null) {
                fullText.append(run.getText(0));
            }
        }

        String updatedText = replacePlaceholders(fullText.toString(), replacements);

        if (!updatedText.contentEquals(fullText)) {
            paragraph.getRuns().forEach(run -> run.setText("", 0));
            var newRun = paragraph.createRun();
            newRun.setText(updatedText);
        }
    }

    private static String replacePlaceholders(String text, Map<String, String> replacements) {
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    public static void saveReport(XWPFDocument document, String reportName) throws IOException {
        fileService.store(convertToMultipartFile(document, reportName), reportName.split("\\.")[0]);
        document.close();
    }

    private static MultipartFile convertToMultipartFile(XWPFDocument document, String fileName)
        throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();
        document.write(byteArrayOutputStream);
        return new ResourceMultipartFile(fileName, fileName,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new ByteArrayResource(byteArrayOutputStream.toByteArray()));
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable63(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table63.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table63.groupName",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table63.groupCode",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table63.NumberOfResults",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        Map<String, Integer> publicationsPerGroup = new TreeMap<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                var groupCode = ClassificationPriorityMapping.getGroupCode(code);
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c)) {
                        publicationsPerGroup.put(groupCode,
                            publicationsPerGroup.getOrDefault(groupCode, 0) + 1);
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        List<List<String>> tableData = new ArrayList<>();
        publicationsPerGroup.forEach((groupCode, publicationCount) -> {
            tableData.add(List.of(String.valueOf(tableData.size() + 1),
                ClassificationPriorityMapping.getGroupNameBasedOnCode(groupCode, locale), groupCode,
                String.valueOf(publicationCount)));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable64(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table64.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table64.publications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        tableData.add(
                            List.of(String.valueOf(tableData.size() + 1), publication.a, code));
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table67.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table67.personalIdentifier",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table67.nameAndSurname",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table67.employmentInstitutionName",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                locale.equals("sr") ?
                    SerbianTransliteration.toCyrillic(assessmentResponse.getPersonName()) :
                    assessmentResponse.getPersonName(),
                getContent(assessmentResponse.getInstitutionName(), locale.toUpperCase()),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67WithPosition(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{header}", messageSource.getMessage("reporting.table67Positions.header",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getFromYear()),
                    String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.table67.personalIdentifier",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.table67.nameAndSurname",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.table67.employmentPosition",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.table67.numberOfSCIPublications",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    numberOfSciPublications.addAndGet(publications.size());
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                locale.equals("sr") ?
                    SerbianTransliteration.toCyrillic(assessmentResponse.getPersonName()) :
                    assessmentResponse.getPersonName(),
                Objects.nonNull(assessmentResponse.getPersonPosition()) ?
                    messageSource.getMessage(assessmentResponse.getPersonPosition().getValue(),
                        new Object[] {}, Locale.forLanguageTag(locale)) :
                    messageSource.getMessage("reporting.shouldCheck", new Object[] {},
                        Locale.forLanguageTag(locale)),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableTopLevelInstitution(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses, String locale) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{heading}", messageSource.getMessage("reporting.tableTopLevelInstitution.header1",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{heading2}", messageSource.getMessage("reporting.tableTopLevelInstitution.header2",
                new Object[] {String.valueOf(assessmentResponses.getFirst().getToYear())},
                Locale.forLanguageTag(locale)),
            "{sci}", messageSource.getMessage("reporting.table.sci",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col1}", messageSource.getMessage("reporting.table.rowNumber",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col2}", messageSource.getMessage("reporting.tableTopLevelInstitution.institution",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col3}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliography",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col4}", messageSource.getMessage("reporting.tableTopLevelInstitution.category",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col5}", messageSource.getMessage("reporting.tableTopLevelInstitution.categories",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col6}",
            messageSource.getMessage("reporting.tableTopLevelInstitution.numberOfPublications",
                new Object[] {}, Locale.forLanguageTag(locale)),
            "{col7}", messageSource.getMessage("reporting.tableTopLevelInstitution.bibliographies",
                new Object[] {}, Locale.forLanguageTag(locale))
        );

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c) &&
                        ClassificationPriorityMapping.isOnSciList(code)) {
                        var institutionIds =
                            assessmentResponse.getPublicationToInstitution().get(publication.c);
                        StringBuilder institutionName = new StringBuilder();
                        if (institutionIds.isEmpty()) {
                            institutionName = new StringBuilder(
                                messageSource.getMessage("reporting.shouldCheck", new Object[] {},
                                    Locale.forLanguageTag(locale)));
                        } else {
                            for (var institutionId : institutionIds) {
                                var index =
                                    organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                                        institutionId);
                                if (index.isEmpty()) {
                                    institutionName = new StringBuilder(
                                        messageSource.getMessage("reporting.shouldCheck",
                                            new Object[] {},
                                            Locale.forLanguageTag(locale)));
                                    break;
                                }

                                if (locale.equals("sr")) {
                                    institutionName.append(
                                            SerbianTransliteration.toCyrillic(index.get().getNameSr()))
                                        .append("\n");
                                } else {
                                    institutionName.append(index.get().getNameOther()).append("\n");
                                }
                            }
                        }

                        tableData.add(
                            List.of(String.valueOf(tableData.size() + 1),
                                institutionName.toString(),
                                publication.a, code));
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        return new Pair<>(replacements, tableData.stream().sorted(Comparator.comparingInt(
            rowA -> ClassificationPriorityMapping.getSciListPriority((rowA.get(3))))).toList());
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTableForAllPublications(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, Pair<String, Integer>> publicationsPerGroup = new TreeMap<>(
            Comparator.comparingInt(ClassificationPriorityMapping::getSciListPriority).reversed());
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                publications.forEach(publication -> {
                    if (!handledPublicationIds.contains(publication.c)) {
                        var existingPair =
                            publicationsPerGroup.getOrDefault(code, new Pair<>("", 0));
                        existingPair.b += 1;
                        existingPair.a += "\n" + existingPair.b + " " + publication.a;
                        publicationsPerGroup.put(code, existingPair);
                        handledPublicationIds.add(publication.c);
                    }
                });
            });
        });

        List<List<String>> tableData = new ArrayList<>();
        publicationsPerGroup.forEach((code, titlesAndCount) -> {
            tableData.add(List.of(String.valueOf(tableData.size() + 1), code,
                String.valueOf(titlesAndCount.b),
                titlesAndCount.a));
        });

        return new Pair<>(new HashMap<>(), tableData);
    }

    private static String getContent(List<MultilingualContentDTO> contentList,
                                     String languageCode) {
        var localisedContent = contentList.stream()
            .filter(mc -> mc.getLanguageTag().equals(languageCode)).findFirst();
        if (localisedContent.isPresent()) {
            return languageCode.equals("SR") ?
                SerbianTransliteration.toCyrillic(localisedContent.get().getContent()) :
                localisedContent.get().getContent();
        }

        return contentList.stream()
            .findFirst()
            .map(mc -> languageCode.equals("SR") ?
                SerbianTransliteration.toCyrillic(mc.getContent()) : mc.getContent())
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }
}
