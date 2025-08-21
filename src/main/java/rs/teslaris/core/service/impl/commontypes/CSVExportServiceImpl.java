package rs.teslaris.core.service.impl.commontypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.converter.document.DocumentPublicationConverter;
import rs.teslaris.core.dto.commontypes.DocumentExportRequestDTO;
import rs.teslaris.core.dto.commontypes.ExportFileType;
import rs.teslaris.core.dto.commontypes.TableExportRequest;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.impl.CSVExportHelper;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.SearchRequestType;
import rs.teslaris.core.util.search.StringUtil;

@Service
@Slf4j
@Transactional
@Traceable
public class CSVExportServiceImpl implements CSVExportService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final DocumentPublicationService documentPublicationService;

    private final CitationService citationService;

    @Value("${csv-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Autowired
    public CSVExportServiceImpl(
        DocumentPublicationIndexRepository documentPublicationIndexRepository,
        PersonIndexRepository personIndexRepository,
        OrganisationUnitIndexRepository organisationUnitIndexRepository,
        OrganisationUnitService organisationUnitService,
        PersonService personService,
        DocumentPublicationService documentPublicationService,
        CitationService citationService) {
        this.documentPublicationIndexRepository = documentPublicationIndexRepository;
        this.personIndexRepository = personIndexRepository;
        this.organisationUnitIndexRepository = organisationUnitIndexRepository;
        this.organisationUnitService = organisationUnitService;
        this.personService = personService;
        this.documentPublicationService = documentPublicationService;
        this.citationService = citationService;
    }

    @Override
    public InputStreamResource exportDocumentsToFile(DocumentExportRequestDTO request) {
        String documentFieldsConfigurationFile = "documentSearchFieldConfiguration.json";

        if (List.of(ExportFileType.CSV, ExportFileType.XLSX)
            .contains(request.getExportFileType())) {
            return exportData(
                request,
                documentPublicationIndexRepository,
                documentFieldsConfigurationFile,
                DocumentPublicationIndexRepository::findDocumentPublicationIndexByDatabaseId,
                (rowData, entity, req) -> CSVExportHelper.addCitationData(rowData, entity,
                    (DocumentExportRequestDTO) req, citationService),
                documentFieldsConfigurationFile
            );
        } else {
            return exportData(request);
        }
    }

    @Override
    public InputStreamResource exportPersonsToCSV(TableExportRequest request) {
        String personFieldsConfigurationFile = "personSearchFieldConfiguration.json";
        return exportData(
            request,
            personIndexRepository,
            personFieldsConfigurationFile,
            PersonIndexRepository::findByDatabaseId,
            (rowData, entity, req) -> {
            },
            personFieldsConfigurationFile
        );
    }

    @Override
    public InputStreamResource exportOrganisationUnitsToCSV(TableExportRequest request) {
        String ouFieldsConfigurationFile = "organisationUnitSearchFieldConfiguration.json";
        return exportData(
            request,
            organisationUnitIndexRepository,
            ouFieldsConfigurationFile,
            OrganisationUnitIndexRepository::findOrganisationUnitIndexByDatabaseId,
            (rowData, entity, req) -> {
            },
            ouFieldsConfigurationFile
        );
    }

    @Override
    public Integer getMaxRecordsPerPage() {
        return maximumExportAmount;
    }

    private InputStreamResource exportData(TableExportRequest request) {
        var documentSpecificFilters =
            handleDocumentSpecificFieldsAndFilters(request, new ArrayList<>(), new ArrayList<>());

        var exportedEntities = new ArrayList<String>();

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request.getEndpointType(),
                request.getEndpointTokenParameters(),
                PageRequest.of(request.getBulkExportOffset(), maximumExportAmount),
                documentPublicationIndexRepository,
                documentSpecificFilters)
                .forEach(entity -> exportedEntities.add(getBibliographicExportEntity(request,
                    documentPublicationService.findOne(entity.getDatabaseId()))));
        } else {
            request.getExportEntityIds().forEach(entityId -> exportedEntities.add(
                getBibliographicExportEntity(request,
                    documentPublicationService.findOne(entityId))));
        }

        var outputStream = new ByteArrayOutputStream();
        try (var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            for (String record : exportedEntities) {
                writer.write(record);
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // should never happen
        }

        return new InputStreamResource(new ByteArrayInputStream(outputStream.toByteArray()));
    }

    private String getBibliographicExportEntity(TableExportRequest request, Document document) {
        return switch (request.getExportFileType()) {
            case BIB -> StringUtil.bibTexEntryToString(
                DocumentPublicationConverter.toBibTeXEntry(document));
            case RIS -> DocumentPublicationConverter.toTaggedFormat(document, true);
            case ENW -> DocumentPublicationConverter.toTaggedFormat(document, false);
            default -> throw new IllegalStateException("Unexpected value: " +
                request.getExportFileType()); // should never happen
        };
    }

    private <T, R extends ElasticsearchRepository<T, ?>> InputStreamResource exportData(
        TableExportRequest request,
        R repository,
        String fieldsConfig,
        BiFunction<R, Integer, Optional<T>> findByDatabaseId,
        TriConsumer<List<String>, T, TableExportRequest> additionalProcessing,
        String configurationFile) {

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = CSVExportHelper.getTableHeaders(request, configurationFile);

        var documentSpecificFilters =
            handleDocumentSpecificFieldsAndFilters(request, tableHeaders, rowsData);

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request.getEndpointType(),
                request.getEndpointTokenParameters(),
                PageRequest.of(request.getBulkExportOffset(), maximumExportAmount), repository,
                documentSpecificFilters)
                .forEach(entity -> {
                    var rowData =
                        CSVExportHelper.constructRowData(entity, request.getColumns(), fieldsConfig,
                            request.getExportLanguage());
                    additionalProcessing.accept(rowData, entity, request);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(entityId -> {
                findByDatabaseId.apply(repository, entityId)
                    .ifPresent(entity -> {
                        var rowData = CSVExportHelper.constructRowData(entity, request.getColumns(),
                            fieldsConfig,
                            request.getExportLanguage());
                        additionalProcessing.accept(rowData, entity, request);
                        rowsData.add(rowData);
                    });
            });
        }

        return CSVExportHelper.createExportFile(rowsData, request.getExportFileType());
    }

    private Triple<ArrayList<DocumentPublicationType>, Integer, Integer> handleDocumentSpecificFieldsAndFilters(
        TableExportRequest request, List<String> tableHeaders, ArrayList<List<String>> rowsData) {
        var allowedDocumentTypes = new ArrayList<DocumentPublicationType>();
        Integer institutionId = null, commissionId = null;

        if (request instanceof DocumentExportRequestDTO) {
            CSVExportHelper.addCitationColumns(tableHeaders, (DocumentExportRequestDTO) request);
            allowedDocumentTypes.addAll(((DocumentExportRequestDTO) request).getAllowedTypes());
            institutionId = ((DocumentExportRequestDTO) request).getInstitutionId();
            commissionId = ((DocumentExportRequestDTO) request).getCommissionId();
        }
        rowsData.add(tableHeaders);

        return new Triple<>(allowedDocumentTypes, institutionId, commissionId);
    }

    @SuppressWarnings("unchecked")
    private <T, R extends ElasticsearchRepository<T, ?>> Page<T> returnBulkDataFromDefinedEndpoint(
        ExportableEndpointType endpointType,
        List<String> endpointTokenParameters,
        Pageable pageable,
        R repository,
        Triple<ArrayList<DocumentPublicationType>, Integer, Integer> documentSpecificFilters
    ) {
        if (endpointType == null) {
            return repository.findAll(pageable);
        }

        return switch (endpointType) {
            case PERSON_SEARCH -> (Page<T>) personService.findPeopleByNameAndEmployment(
                endpointTokenParameters, pageable, false, null, false);
            case DOCUMENT_SEARCH, THESIS_SIMPLE_SEARCH ->
                (Page<T>) documentPublicationService.searchDocumentPublications(
                    endpointTokenParameters, pageable,
                    SearchRequestType.SIMPLE, documentSpecificFilters.b, documentSpecificFilters.c,
                    documentSpecificFilters.a);
            case DOCUMENT_ADVANCED_SEARCH, THESIS_ADVANCED_SEARCH ->
                (Page<T>) documentPublicationService.searchDocumentPublications(
                    endpointTokenParameters, pageable,
                    SearchRequestType.ADVANCED, documentSpecificFilters.b,
                    documentSpecificFilters.c, documentSpecificFilters.a);
            case ORGANISATION_UNIT_SEARCH ->
                (Page<T>) organisationUnitService.searchOrganisationUnits(
                    Arrays.stream(endpointTokenParameters.getFirst().split("tokens="))
                        .filter(t -> !t.isBlank()).collect(Collectors.toList()), pageable,
                    SearchRequestType.SIMPLE, null,
                    Integer.parseInt(endpointTokenParameters.getLast()), null, null);
            case PERSON_OUTPUTS -> (Page<T>) documentPublicationService.findResearcherPublications(
                Integer.parseInt(endpointTokenParameters.getFirst()), null,
                Arrays.stream(endpointTokenParameters.getLast().split("tokens="))
                    .filter(t -> !t.isBlank()).toList(), documentSpecificFilters.a, pageable);
            case ORGANISATION_UNIT_OUTPUTS ->
                (Page<T>) documentPublicationService.findPublicationsForOrganisationUnit(
                    Integer.parseInt(endpointTokenParameters.getFirst()),
                    Arrays.stream(endpointTokenParameters.getLast().split("tokens="))
                        .filter(t -> !t.isBlank()).toList(), documentSpecificFilters.a, pageable);
            case ORGANISATION_UNIT_EMPLOYEES ->
                (Page<T>) personService.findPeopleForOrganisationUnit(
                    Integer.parseInt(endpointTokenParameters.getFirst()),
                    Arrays.stream(endpointTokenParameters.get(1).split("tokens="))
                        .filter(t -> !t.isBlank()).toList(), pageable,
                    Boolean.parseBoolean(endpointTokenParameters.getLast()));
        };
    }
}
