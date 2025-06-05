package rs.teslaris.core.service.impl;

import com.opencsv.CSVWriter;
import jakarta.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequestDTO;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@Component
@Transactional
@Slf4j
public class CSVExportHelper {

    private static final Map<Function<CitationResponseDTO, String>, String> CITATION_FORMATS =
        Map.of(
            CitationResponseDTO::getApa, "APA",
            CitationResponseDTO::getMla, "MLA",
            CitationResponseDTO::getChicago, "Chicago",
            CitationResponseDTO::getHarvard, "Harvard",
            CitationResponseDTO::getVancouver, "Vancouver"
        );
    private static SearchFieldsLoader searchFieldsLoader;
    private static PublicationSeriesService publicationSeriesService;
    private static EventService eventService;


    @Autowired
    public CSVExportHelper(SearchFieldsLoader searchFieldsLoader,
                           PublicationSeriesService publicationSeriesService,
                           EventService eventService) {
        CSVExportHelper.searchFieldsLoader = searchFieldsLoader;
        CSVExportHelper.publicationSeriesService = publicationSeriesService;
        CSVExportHelper.eventService = eventService;
    }

    public static InputStreamResource createExportFile(List<List<String>> rowsData,
                                                       ExportFileType exportFileType) {
        return switch (exportFileType) {
            case CSV -> createCSVInputStream(rowsData);
            case XLS -> createXLSInputStream(rowsData);
        };
    }

    @Nullable
    public static Object getFieldValueByElasticsearchName(Object obj, String indexFieldName) {
        Class<?> clazz = obj.getClass();

        for (var field : clazz.getDeclaredFields()) {
            var fieldAnnotation = field.getAnnotation(Field.class);
            if (fieldAnnotation != null && fieldAnnotation.name().equals(indexFieldName)) {
                String javaFieldName = field.getName(); // Get the Java field name

                String getterName = "get" + Character.toUpperCase(javaFieldName.charAt(0)) +
                    javaFieldName.substring(1);

                Method getterMethod;
                try {
                    getterMethod = clazz.getMethod(getterName);
                    return getterMethod.invoke(obj);
                } catch (NoSuchMethodException | InvocationTargetException |
                         IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        log.warn("No field found for field name: {}", indexFieldName);
        return null;
    }

    private static String applyRule(String value, String fieldName, String configFileName,
                                    String language) {
        var fieldConfiguration =
            searchFieldsLoader.getConfiguration(configFileName).fields().stream()
                .filter(field -> field.fieldName().equalsIgnoreCase(fieldName)).findFirst();

        if (fieldConfiguration.isEmpty() || value.isEmpty()) {
            return "";
        }

        return switch (Objects.requireNonNullElse(fieldConfiguration.get().rule(), "NONE")) {
            case "POSITIVE" -> Integer.parseInt(value) > 0 ? value : "";
            case "POSITIVE_OR_ZERO" -> Integer.parseInt(value) >= 0 ? value : "";
            case "NEGATIVE" -> Integer.parseInt(value) < 0 ? value : "";
            case "INLINE" -> value.replace("\n", "; ");
            case "JOURNAL" -> {
                var journal = publicationSeriesService.findOne(Integer.parseInt(value));
                yield
                    MultilingualContentConverter.getLocalizedContent(journal.getTitle(), language) +
                        journal.getIssnString();
            }
            case "EVENT" -> MultilingualContentConverter.getLocalizedContent(
                eventService.findOne(Integer.parseInt(value)).getName(), language);
            default -> value.replace("\n", "");
        };
    }

    public static List<String> constructRowData(Object entity, List<String> columnNames,
                                                String configFileName, String language) {
        var rowData = new ArrayList<String>();

        columnNames.forEach(columnName -> {
            var value = getFieldValueByElasticsearchName(entity, columnName);
            rowData.add(applyRule(Objects.requireNonNullElse(value, "").toString(), columnName,
                configFileName, language));
        });

        return rowData;
    }

    private static InputStreamResource createCSVInputStream(List<List<String>> rowsData) {
        try {
            var byteArrayOutputStream = new ByteArrayOutputStream();
            var outputStreamWriter = new OutputStreamWriter(byteArrayOutputStream);
            var csvWriter = new CSVWriter(outputStreamWriter);

            for (List<String> row : rowsData) {
                csvWriter.writeNext(row.toArray(new String[0]));
            }

            csvWriter.close();
            return new InputStreamResource(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("Error generating CSV file", e);
        }
    }

    private static InputStreamResource createXLSInputStream(List<List<String>> rowsData) {
        try {
            var workbook = new HSSFWorkbook();
            var sheet = workbook.createSheet("Export Data");

            for (int rowIndex = 0; rowIndex < rowsData.size(); rowIndex++) {
                var row = sheet.createRow(rowIndex);
                List<String> rowData = rowsData.get(rowIndex);

                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    var cell = row.createCell(colIndex);
                    cell.setCellValue(rowData.get(colIndex));
                }
            }

            var byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);
            workbook.close();

            return new InputStreamResource(
                new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException("Error generating XLS file", e);
        }
    }

    public static List<String> getTableHeaders(CSVExportRequest request,
                                               String configurationFile) {
        return request.getColumns().stream()
            .map(searchFieldName -> searchFieldsLoader.getSearchFieldLocalizedName(
                configurationFile, searchFieldName, request.getExportLanguage()))
            .collect(Collectors.toList());
    }

    public static void addCitationData(List<String> rowData, DocumentPublicationIndex document,
                                       DocumentCSVExportRequestDTO request,
                                       CitationService citationService) {
        if (!shouldIncludeCitation(request)) {
            return;
        }

        var citationData = citationService.craftCitations(document, request.getExportLanguage());

        CITATION_FORMATS.forEach((getter, format) -> {
            if (requestFormatEnabled(request, format)) {
                rowData.add(getter.apply(citationData));
            }
        });
    }

    public static void addCitationColumns(List<String> tableHeaders,
                                          DocumentCSVExportRequestDTO request) {
        CITATION_FORMATS.values().forEach(format -> {
            if (requestFormatEnabled(request, format)) {
                tableHeaders.add(format);
            }
        });
    }

    private static boolean shouldIncludeCitation(DocumentCSVExportRequestDTO request) {
        return Objects.requireNonNullElse(request.getApa(), false) ||
            Objects.requireNonNullElse(request.getMla(), false) ||
            Objects.requireNonNullElse(request.getChicago(), false) ||
            Objects.requireNonNullElse(request.getHarvard(), false) ||
            Objects.requireNonNullElse(request.getVancouver(), false);
    }

    private static boolean requestFormatEnabled(DocumentCSVExportRequestDTO request,
                                                String format) {
        return switch (format) {
            case "APA" -> Objects.requireNonNullElse(request.getApa(), false);
            case "MLA" -> Objects.requireNonNullElse(request.getMla(), false);
            case "Chicago" -> Objects.requireNonNullElse(request.getChicago(), false);
            case "Harvard" -> Objects.requireNonNullElse(request.getHarvard(), false);
            case "Vancouver" -> Objects.requireNonNullElse(request.getVancouver(), false);
            default -> false;
        };
    }
}
