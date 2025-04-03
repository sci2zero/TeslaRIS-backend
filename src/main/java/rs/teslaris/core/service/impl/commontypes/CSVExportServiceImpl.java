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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.EventService;
import rs.teslaris.core.service.interfaces.document.PublicationSeriesService;
import rs.teslaris.core.util.search.SearchFieldsLoader;

@Service
@Slf4j
public class CSVExportServiceImpl implements CSVExportService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final SearchFieldsLoader searchFieldsLoader;

    private final CitationService citationService;

    private final PublicationSeriesService publicationSeriesService;

    private final EventService eventService;

    private final String DOCUMENT_FIELDS_CONFIGURATION_FILE =
        "documentSearchFieldConfiguration.json";

    private final String PERSON_FIELDS_CONFIGURATION_FILE =
        "personSearchFieldConfiguration.json";

    private final String OU_FIELDS_CONFIGURATION_FILE =
        "organisationUnitSearchFieldConfiguration.json";
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
        PersonIndexRepository personIndexRepository,
        OrganisationUnitIndexRepository organisationUnitIndexRepository,
        SearchFieldsLoader searchFieldsLoader, CitationService citationService,
        PublicationSeriesService publicationSeriesService, EventService eventService)
        throws IOException {
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.personIndexRepository = personIndexRepository;
        this.organisationUnitIndexRepository = organisationUnitIndexRepository;
        this.searchFieldsLoader = searchFieldsLoader;
        this.citationService = citationService;
        this.publicationSeriesService = publicationSeriesService;
        this.eventService = eventService;
        this.searchFieldsLoader.loadConfiguration(DOCUMENT_FIELDS_CONFIGURATION_FILE);
        this.searchFieldsLoader.loadConfiguration(PERSON_FIELDS_CONFIGURATION_FILE);
        this.searchFieldsLoader.loadConfiguration(OU_FIELDS_CONFIGURATION_FILE);
    }

    @Override
    public InputStreamResource exportDocumentsToCSV(DocumentCSVExportRequest request) {
        return exportData(
            request,
            documentPublicationIndexRepository,
            DOCUMENT_FIELDS_CONFIGURATION_FILE,
            DocumentPublicationIndexRepository::findDocumentPublicationIndexByDatabaseId,
            (rowData, entity, req) -> addCitationData(rowData, entity,
                (DocumentCSVExportRequest) req, citationService),
            DOCUMENT_FIELDS_CONFIGURATION_FILE
        );
    }

    @Override
    public InputStreamResource exportPersonsToCSV(CSVExportRequest request) {
        return exportData(
            request,
            personIndexRepository,
            PERSON_FIELDS_CONFIGURATION_FILE,
            PersonIndexRepository::findByDatabaseId,
            (rowData, entity, req) -> {
            },
            PERSON_FIELDS_CONFIGURATION_FILE
        );
    }

    @Override
    public InputStreamResource exportOrganisationUnitsToCSV(CSVExportRequest request) {
        return exportData(
            request,
            organisationUnitIndexRepository,
            OU_FIELDS_CONFIGURATION_FILE,
            OrganisationUnitIndexRepository::findOrganisationUnitIndexByDatabaseId,
            (rowData, entity, req) -> {
            },
            OU_FIELDS_CONFIGURATION_FILE
        );
    }

    @Override
    public Integer getMaxRecordsPerPage() {
        return maximumExportAmount;
    }

    private <T, R extends ElasticsearchRepository<T, ?>> InputStreamResource exportData(
        CSVExportRequest request,
        R repository,
        String fieldsConfig,
        BiFunction<R, Integer, Optional<T>> findByDatabaseId,
        TriConsumer<List<String>, T, CSVExportRequest> additionalProcessing,
        String configurationFile) {

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = getTableHeaders(request, configurationFile);
        if (request instanceof DocumentCSVExportRequest) {
            addCitationColumns(tableHeaders, (DocumentCSVExportRequest) request);
        }
        rowsData.add(tableHeaders);

        if (request.getExportMaxPossibleAmount()) {
            repository.findAll(PageRequest.of(request.getBulkExportOffset(), maximumExportAmount))
                .forEach(entity -> {
                    var rowData = constructRowData(entity, request.getColumns(), fieldsConfig,
                        request.getExportLanguage());
                    additionalProcessing.accept(rowData, entity, request);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(entityId -> {
                findByDatabaseId.apply(repository, entityId)
                    .ifPresent(entity -> {
                        var rowData = constructRowData(entity, request.getColumns(), fieldsConfig,
                            request.getExportLanguage());
                        additionalProcessing.accept(rowData, entity, request);
                        rowsData.add(rowData);
                    });
            });
        }

        return switch (request.getExportFileType()) {
            case CSV -> createCSVInputStream(rowsData);
            case XLS -> createXLSInputStream(rowsData);
        };
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

    private List<String> constructRowData(Object entity, List<String> columnNames,
                                          String configFileName, String language) {
        var rowData = new ArrayList<String>();

        columnNames.forEach(columnName -> {
            var value = getFieldValueByElasticsearchName(entity, columnName);
            rowData.add(applyRule(Objects.requireNonNullElse(value, "").toString(), columnName,
                configFileName, language));
        });

        return rowData;
    }

    private String applyRule(String value, String fieldName, String configFileName,
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

    private InputStreamResource createXLSInputStream(List<List<String>> rowsData) {
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

    private List<String> getTableHeaders(CSVExportRequest request, String configurationFile) {
        return request.getColumns().stream()
            .map(searchFieldName -> searchFieldsLoader.getSearchFieldLocalizedName(
                configurationFile, searchFieldName, request.getExportLanguage()))
            .collect(Collectors.toList());
    }
}
