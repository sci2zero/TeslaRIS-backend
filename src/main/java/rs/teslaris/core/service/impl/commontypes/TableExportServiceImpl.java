package rs.teslaris.core.service.impl.commontypes;

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
import rs.teslaris.core.dto.commontypes.TableExportRequestDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.service.impl.TableExportHelper;
import rs.teslaris.core.service.interfaces.commontypes.TableExportService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
@Slf4j
@Transactional
@Traceable
public class TableExportServiceImpl implements TableExportService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final OrganisationUnitService organisationUnitService;

    private final PersonService personService;

    private final DocumentPublicationService documentPublicationService;

    private final CitationService citationService;

    @Value("${table-export.maximum-export-amount}")
    private Integer maximumExportAmount;


    @Autowired
    public TableExportServiceImpl(
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
                (rowData, entity, req) -> TableExportHelper.addCitationData(rowData, entity,
                    (DocumentExportRequestDTO) req, citationService),
                documentFieldsConfigurationFile
            );
        } else {
            return exportData(request);
        }
    }

    @Override
    public InputStreamResource exportPersonsToCSV(TableExportRequestDTO request) {
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
    public InputStreamResource exportOrganisationUnitsToCSV(TableExportRequestDTO request) {
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

    private InputStreamResource exportData(TableExportRequestDTO request) {
        var documentSpecificFilters =
            handleDocumentSpecificFieldsAndFilters(request, new ArrayList<>(), new ArrayList<>());

        var exportedEntities = new ArrayList<String>();

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request.getEndpointType(),
                request.getEndpointTokenParameters(),
                PageRequest.of(request.getBulkExportOffset(), maximumExportAmount),
                documentPublicationIndexRepository,
                documentSpecificFilters)
                .forEach(entity -> exportedEntities.add(
                    DocumentPublicationConverter.getBibliographicExportEntity(request,
                        documentPublicationService.findOne(entity.getDatabaseId()))));
        } else {
            request.getExportEntityIds().forEach(entityId -> exportedEntities.add(
                DocumentPublicationConverter.getBibliographicExportEntity(request,
                    documentPublicationService.findOne(entityId))));
        }

        return TableExportHelper.createExportFile(exportedEntities, request.getExportFileType());
    }

    private <T, R extends ElasticsearchRepository<T, ?>> InputStreamResource exportData(
        TableExportRequestDTO request,
        R repository,
        String fieldsConfig,
        BiFunction<R, Integer, Optional<T>> findByDatabaseId,
        TriConsumer<List<String>, T, TableExportRequestDTO> additionalProcessing,
        String configurationFile) {

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = TableExportHelper.getTableHeaders(request, configurationFile);

        var documentSpecificFilters =
            handleDocumentSpecificFieldsAndFilters(request, tableHeaders, rowsData);

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request.getEndpointType(),
                request.getEndpointTokenParameters(),
                PageRequest.of(request.getBulkExportOffset(), maximumExportAmount), repository,
                documentSpecificFilters)
                .forEach(entity -> {
                    var rowData =
                        TableExportHelper.constructRowData(entity, request.getColumns(),
                            fieldsConfig,
                            request.getExportLanguage());
                    additionalProcessing.accept(rowData, entity, request);
                    rowsData.add(rowData);
                });
        } else {
            request.getExportEntityIds().forEach(entityId -> {
                findByDatabaseId.apply(repository, entityId)
                    .ifPresent(entity -> {
                        var rowData =
                            TableExportHelper.constructRowData(entity, request.getColumns(),
                                fieldsConfig,
                                request.getExportLanguage());
                        additionalProcessing.accept(rowData, entity, request);
                        rowsData.add(rowData);
                    });
            });
        }

        return TableExportHelper.createExportFile(rowsData, request.getExportFileType());
    }

    private Triple<ArrayList<DocumentPublicationType>, Integer, Integer> handleDocumentSpecificFieldsAndFilters(
        TableExportRequestDTO request, List<String> tableHeaders,
        ArrayList<List<String>> rowsData) {
        var allowedDocumentTypes = new ArrayList<DocumentPublicationType>();
        Integer institutionId = null, commissionId = null;

        if (request instanceof DocumentExportRequestDTO) {
            TableExportHelper.addCitationColumns(tableHeaders, (DocumentExportRequestDTO) request);
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
                    Integer.parseInt(endpointTokenParameters.getLast()), null, null, null);
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
