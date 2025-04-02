package rs.teslaris.core.service.impl.commontypes;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@Service
@Slf4j
public class CSVExportServiceImpl implements CSVExportService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final SearchFieldsLoader searchFieldsLoader;

    private final CitationService citationService;

    private final String DOCUMENT_FIELDS_CONFIGURATION_FILE =
        "documentSearchFieldConfiguration.json";
    private final Map<Function<CitationResponseDTO, String>, String> CITATION_FORMATS = Map.of(
        CitationResponseDTO::getApa, "APA",
        CitationResponseDTO::getMla, "MLA",
        CitationResponseDTO::getChicago, "Chicago",
        CitationResponseDTO::getHarvard, "Harvard",
        CitationResponseDTO::getVancouver, "Vancouver"
    );
    @Value("${csv-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Autowired
    public CSVExportServiceImpl(
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        SearchFieldsLoader searchFieldsLoader, CitationService citationService) throws IOException {
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.searchFieldsLoader = searchFieldsLoader;
        this.citationService = citationService;
        this.searchFieldsLoader.loadConfiguration(DOCUMENT_FIELDS_CONFIGURATION_FILE);
    }

    @Override
    public InputStreamResource exportDocumentsToCSV(DocumentCSVExportRequest request) {
        var rowsData = new ArrayList<List<String>>();

        var tableHeaders = request.getColumns().stream()
            .map(searchFieldName -> searchFieldsLoader.getSearchFieldLocalizedName(
                DOCUMENT_FIELDS_CONFIGURATION_FILE, searchFieldName, request.getExportLanguage()))
            .toList();

        var headersWithCitations = new ArrayList<>(tableHeaders);
        addCitationColumns(headersWithCitations, request);
        rowsData.add(headersWithCitations);

        if (request.getExportMaxPossibleAmount()) {
            documentPublicationIndexRepository.findAll(
                    PageRequest.of(request.getBulkExportOffset(), maximumExportAmount))
                .forEach(document -> {
                    var rowData = constructRowData(document, request.getColumns());
                    addCitationData(rowData, document, request, citationService);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(documentId -> {
                documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                        documentId)
                    .ifPresent(doc -> {
                        var rowData = constructRowData(doc, request.getColumns());
                        addCitationData(rowData, doc, request, citationService);
                        rowsData.add(rowData);
                    });
            });
        }

        return createCSVInputStream(rowsData);
    }

    @Override
    public InputStreamResource exportPersonsToCSV(CSVExportRequest request) {
        return null;
    }

    @Override
    public InputStreamResource exportOrganisationUnitsToCSV(CSVExportRequest request) {
        return null;
    }

    @Nullable
    public Object getFieldValueByElasticsearchName(Object obj, String indexFieldName) {
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

    private List<String> constructRowData(Object entity, List<String> columnNames) {
        var rowData = new ArrayList<String>();

        columnNames.forEach(columnName -> {
            rowData.add(
                Objects.requireNonNullElse(getFieldValueByElasticsearchName(entity, columnName), "")
                    .toString());
        });

        return rowData;
    }

    private InputStreamResource createCSVInputStream(List<List<String>> rowsData) {
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

    public void addCitationData(List<String> rowData, DocumentPublicationIndex document,
                                DocumentCSVExportRequest request, CitationService citationService) {
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

    public void addCitationColumns(List<String> tableHeaders, DocumentCSVExportRequest request) {
        CITATION_FORMATS.values().forEach(format -> {
            if (requestFormatEnabled(request, format)) {
                tableHeaders.add(format);
            }
        });
    }

    private boolean shouldIncludeCitation(DocumentCSVExportRequest request) {
        return Objects.requireNonNullElse(request.getApa(), false) ||
            Objects.requireNonNullElse(request.getMla(), false) ||
            Objects.requireNonNullElse(request.getChicago(), false) ||
            Objects.requireNonNullElse(request.getHarvard(), false) ||
            Objects.requireNonNullElse(request.getVancouver(), false);
    }

    private boolean requestFormatEnabled(DocumentCSVExportRequest request, String format) {
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
