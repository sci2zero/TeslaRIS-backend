package rs.teslaris.core.util;

import com.ibm.icu.text.Transliterator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.dto.EnrichedResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.util.ClassificationPriorityMapping;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Component
public class ReportGenerationUtil {

    private static final Transliterator LATIN_TO_CYRILLIC =
        Transliterator.getInstance("Latin-Cyrillic");

    private static MessageSource messageSource;


    @Autowired
    public ReportGenerationUtil(MessageSource messageSource) {
        ReportGenerationUtil.messageSource = messageSource;
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
        for (var paragraph : document.getParagraphs()) {
            for (var run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    for (Map.Entry<String, String> entry : replacements.entrySet()) {
                        text = text.replace(entry.getKey(), entry.getValue());
                    }
                    run.setText(text, 0);
                }
            }
        }
    }

    public static void saveReport(XWPFDocument document, String reportName) throws IOException {
        var fos = new FileOutputStream("src/main/resources/reportTemplates/" + reportName);
        document.write(fos);
        fos.close();
        document.close();
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable63(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{assessmentYear}", String.valueOf(assessmentResponses.getFirst().getFromYear())
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
                ClassificationPriorityMapping.getGroupNameBasedOnCode(groupCode), groupCode,
                String.valueOf(publicationCount)));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{fromYear}", String.valueOf(assessmentResponses.getFirst().getFromYear()),
            "{toYear}", String.valueOf(assessmentResponses.getFirst().getToYear())
        );

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    publications.forEach(publication -> {
                        if (!handledPublicationIds.contains(publication.c)) {
                            numberOfSciPublications.addAndGet(1);
                            handledPublicationIds.add(publication.c);
                        }
                    });
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                LATIN_TO_CYRILLIC.transliterate(assessmentResponse.getPersonName()),
                getContent(assessmentResponse.getInstitutionName(), "SR"),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    public static Pair<Map<String, String>, List<List<String>>> constructDataForTable67WithPosition(
        List<EnrichedResearcherAssessmentResponseDTO> assessmentResponses) {
        if (assessmentResponses.isEmpty()) {
            return new Pair<>(new HashMap<>(), new ArrayList<>());
        }

        Map<String, String> replacements = Map.of(
            "{fromYear}", String.valueOf(assessmentResponses.getFirst().getFromYear()),
            "{toYear}", String.valueOf(assessmentResponses.getFirst().getToYear())
        );

        List<List<String>> tableData = new ArrayList<>();
        Set<Integer> handledPublicationIds = new HashSet<>();
        assessmentResponses.forEach(assessmentResponse -> {
            AtomicInteger numberOfSciPublications = new AtomicInteger();
            assessmentResponse.getPublicationsPerCategory().forEach((code, publications) -> {
                if (ClassificationPriorityMapping.isOnSciList(code)) {
                    publications.forEach(publication -> {
                        if (!handledPublicationIds.contains(publication.c)) {
                            numberOfSciPublications.addAndGet(1);
                            handledPublicationIds.add(publication.c);
                        }
                    });
                }
            });
            tableData.add(List.of(String.valueOf(tableData.size() + 1), "",
                LATIN_TO_CYRILLIC.transliterate(assessmentResponse.getPersonName()),
                Objects.nonNull(assessmentResponse.getPersonPosition()) ?
                    messageSource.getMessage(assessmentResponse.getPersonPosition().getValue(),
                        new Object[] {}, Locale.forLanguageTag("sr")) :
                    messageSource.getMessage("reporting.shouldCheck", new Object[] {},
                        Locale.forLanguageTag("sr")),
                String.valueOf(numberOfSciPublications.get())));
        });

        return new Pair<>(replacements, tableData);
    }

    private static String getContent(List<MultilingualContentDTO> contentList,
                                     String languageCode) {
        var localisedContent = contentList.stream()
            .filter(mc -> mc.getLanguageTag().equals(languageCode)).findFirst();
        if (localisedContent.isPresent()) {
            return LATIN_TO_CYRILLIC.transliterate(localisedContent.get().getContent());
        }

        return contentList.stream()
            .findFirst()
            .map(mc -> LATIN_TO_CYRILLIC.transliterate(mc.getContent()))
            .orElseThrow(() -> new NotFoundException("Missing container title"));
    }
}
