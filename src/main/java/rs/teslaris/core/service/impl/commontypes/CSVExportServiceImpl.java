package rs.teslaris.core.service.impl.commontypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
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
import rs.teslaris.core.dto.commontypes.CSVExportRequest;
import rs.teslaris.core.dto.commontypes.DocumentCSVExportRequest;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.ExportableEndpointType;
import rs.teslaris.core.service.impl.CSVExportHelper;
import rs.teslaris.core.service.interfaces.commontypes.CSVExportService;
import rs.teslaris.core.service.interfaces.document.CitationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
@Slf4j
@Transactional
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
    public InputStreamResource exportDocumentsToCSV(DocumentCSVExportRequest request) {
        String documentFieldsConfigurationFile = "documentSearchFieldConfiguration.json";
        return exportData(
            request,
            documentPublicationIndexRepository,
            documentFieldsConfigurationFile,
            DocumentPublicationIndexRepository::findDocumentPublicationIndexByDatabaseId,
            (rowData, entity, req) -> CSVExportHelper.addCitationData(rowData, entity,
                (DocumentCSVExportRequest) req, citationService),
            documentFieldsConfigurationFile
        );
    }

    @Override
    public InputStreamResource exportPersonsToCSV(CSVExportRequest request) {
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
    public InputStreamResource exportOrganisationUnitsToCSV(CSVExportRequest request) {
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

    private <T, R extends ElasticsearchRepository<T, ?>> InputStreamResource exportData(
        CSVExportRequest request,
        R repository,
        String fieldsConfig,
        BiFunction<R, Integer, Optional<T>> findByDatabaseId,
        TriConsumer<List<String>, T, CSVExportRequest> additionalProcessing,
        String configurationFile) {

        var rowsData = new ArrayList<List<String>>();
        var tableHeaders = CSVExportHelper.getTableHeaders(request, configurationFile);
        if (request instanceof DocumentCSVExportRequest) {
            CSVExportHelper.addCitationColumns(tableHeaders, (DocumentCSVExportRequest) request);
        }
        rowsData.add(tableHeaders);

        if (request.getExportMaxPossibleAmount()) {
            returnBulkDataFromDefinedEndpoint(request.getEndpointType(),
                request.getEndpointTokenParameters(),
                PageRequest.of(request.getBulkExportOffset(), maximumExportAmount), repository)
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

    @SuppressWarnings("unchecked")
    private <T, R extends ElasticsearchRepository<T, ?>> Page<T> returnBulkDataFromDefinedEndpoint(
        ExportableEndpointType endpointType,
        List<String> endpointTokenParameters,
        Pageable pageable,
        R repository
    ) {
        if (endpointType == null) {
            return repository.findAll(pageable);
        }

        return switch (endpointType) {
            case PERSON_SEARCH -> (Page<T>) personService.findPeopleByNameAndEmployment(
                endpointTokenParameters, pageable, false, null);
            case DOCUMENT_SEARCH, THESIS_SIMPLE_SEARCH, THESIS_ADVANCED_SEARCH ->
                (Page<T>) documentPublicationService.searchDocumentPublications(
                    endpointTokenParameters, pageable,
                    SearchRequestType.SIMPLE, null, null, null);
            case ORGANISATION_UNIT_SEARCH ->
                (Page<T>) organisationUnitService.searchOrganisationUnits(
                    endpointTokenParameters, pageable,
                    SearchRequestType.SIMPLE, null, null);
            case PERSON_OUTPUTS -> (Page<T>) documentPublicationService.findResearcherPublications(
                Integer.parseInt(endpointTokenParameters.getFirst()), null, pageable);
            case ORGANISATION_UNIT_OUTPUTS ->
                (Page<T>) documentPublicationService.findPublicationsForOrganisationUnit(
                    Integer.parseInt(endpointTokenParameters.getFirst()), pageable);
            case ORGANISATION_UNIT_EMPLOYEES ->
                (Page<T>) personService.findPeopleForOrganisationUnit(
                    Integer.parseInt(endpointTokenParameters.getFirst()), pageable, false);
        };
    }
}
