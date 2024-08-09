package rs.teslaris.core.exporter.service.impl;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exporter.model.common.BaseExportEntity;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportEvent;
import rs.teslaris.core.exporter.model.common.ExportOrganisationUnit;
import rs.teslaris.core.exporter.model.common.ExportPerson;
import rs.teslaris.core.exporter.model.converter.ExportDocumentConverter;
import rs.teslaris.core.exporter.model.converter.ExportEventConverter;
import rs.teslaris.core.exporter.model.converter.ExportOrganisationUnitConverter;
import rs.teslaris.core.exporter.model.converter.ExportPersonConverter;
import rs.teslaris.core.exporter.service.interfaces.OutboundExportService;
import rs.teslaris.core.exporter.util.ExportDataFormat;
import rs.teslaris.core.exporter.util.ExportHandlersConfigurationLoader;
import rs.teslaris.core.exporter.util.OAIErrorFactory;
import rs.teslaris.core.importer.model.oaipmh.common.Description;
import rs.teslaris.core.importer.model.oaipmh.common.GetRecord;
import rs.teslaris.core.importer.model.oaipmh.common.Header;
import rs.teslaris.core.importer.model.oaipmh.common.Identify;
import rs.teslaris.core.importer.model.oaipmh.common.ListMetadataFormats;
import rs.teslaris.core.importer.model.oaipmh.common.ListRecords;
import rs.teslaris.core.importer.model.oaipmh.common.ListSets;
import rs.teslaris.core.importer.model.oaipmh.common.Metadata;
import rs.teslaris.core.importer.model.oaipmh.common.MetadataFormat;
import rs.teslaris.core.importer.model.oaipmh.common.OAIIdentifier;
import rs.teslaris.core.importer.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.importer.model.oaipmh.common.Record;
import rs.teslaris.core.importer.model.oaipmh.common.ResumptionToken;
import rs.teslaris.core.importer.model.oaipmh.common.ServiceDescription;
import rs.teslaris.core.importer.model.oaipmh.common.Set;
import rs.teslaris.core.importer.model.oaipmh.common.Toolkit;
import rs.teslaris.core.importer.model.oaipmh.event.Event;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnit;
import rs.teslaris.core.importer.model.oaipmh.person.Person;
import rs.teslaris.core.importer.model.oaipmh.publication.AbstractPublication;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@Service
@RequiredArgsConstructor
public class OutboundExportServiceImpl implements OutboundExportService {

    private final MongoTemplate mongoTemplate;
    private final int PAGE_SIZE = 10;
    @Value("${export.base.url}")
    private String baseUrl;
    @Value("${export.repo.name}")
    private String repositoryName;
    @Value("${export.admin.email}")
    private String adminEmail;
    @Value("${client.address}")
    private String frontendURL;

    @Override
    public ListRecords listRequestedRecords(String handler, String metadataPrefix,
                                            String from, String until, String requestedSet,
                                            OAIPMHResponse response, int page) {
        if (Objects.isNull(metadataPrefix) || metadataPrefix.isBlank() ||
            Objects.isNull(requestedSet) || requestedSet.isBlank() ||
            Objects.isNull(from) || from.isBlank() ||
            Objects.isNull(until) || until.isBlank()) {
            response.setError(OAIErrorFactory.constructBadArgumentError());
            return null;
        }

        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        if (handlerConfiguration.get().metadataFormats().stream()
            .noneMatch(format -> format.equals(metadataPrefix))) {
            response.setError(OAIErrorFactory.constructFormatError(metadataPrefix));
            return null;
        }

        var matchedSet = handlerConfiguration.get().sets().stream()
            .filter(set -> set.setSpec().equals(requestedSet))
            .findFirst();

        if (matchedSet.isEmpty() || matchedSet.get().commonEntityClass().equals("NONE")) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

        Class<?> recordClass = null;
        Class<?> converterClass = null;

        try {
            recordClass = Class.forName(
                "rs.teslaris.core.exporter.model.common." + matchedSet.get().commonEntityClass());
            converterClass = Class.forName("rs.teslaris.core.exporter.model.converter." +
                matchedSet.get().commonEntityClass() + "Converter");
        } catch (ClassNotFoundException e) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

        var listRecords = new ListRecords();
        listRecords.setRecords(new ArrayList<>());

        var records =
            findRequestedRecords(recordClass, from, until, page, handlerConfiguration.get());

        for (var fetchedRecordEntity : records) {
            var record = new Record();
            listRecords.getRecords().add(record);
            var metadata = new Metadata();

            record.setHeader(constructOaiResponseHeader(handlerConfiguration.get(),
                (BaseExportEntity) fetchedRecordEntity,
                "oai:CRIS.UNS:" + (!matchedSet.get().identifierSetSpec().isBlank() ?
                    (matchedSet.get().identifierSetSpec() + "/") : "") + "(TESLARIS)" +
                    ((BaseExportEntity) fetchedRecordEntity).getDatabaseId(),
                matchedSet.get().identifierSetSpec()));

            if (Objects.nonNull(record.getHeader().getStatus()) &&
                record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
                return listRecords;
            }

            setMetadataFieldsInGivenFormat(matchedSet.get().identifierSetSpec(), recordClass,
                converterClass, ExportDataFormat.fromStringValue(metadataPrefix), metadata,
                fetchedRecordEntity);

            record.setMetadata(metadata);
        }

        listRecords.setResumptionToken(
            constructResumptionToken(from, until, page, requestedSet, metadataPrefix));
        return listRecords;
    }

    @Override
    public GetRecord listRequestedRecord(String handler, String metadataPrefix,
                                         String identifier, OAIPMHResponse response) {
        if (Objects.isNull(metadataPrefix) || metadataPrefix.isBlank() ||
            Objects.isNull(identifier) || identifier.isBlank()) {
            response.setError(OAIErrorFactory.constructBadArgumentError());
            return null;
        }

        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        if (handlerConfiguration.get().metadataFormats().stream()
            .noneMatch(format -> format.equals(metadataPrefix))) {
            response.setError(OAIErrorFactory.constructFormatError(metadataPrefix));
            return null;
        }

        var getRecord = new GetRecord();
        var record = new Record();
        getRecord.setRecord(record);
        var metadata = new Metadata();
        var metadataFormat = ExportDataFormat.fromStringValue(metadataPrefix);

        Class<?> recordClass = null;
        Class<?> converterClass = null;

        String set;
        if (identifier.contains("/")) {
            set = identifier.split("/")[0].split(":")[2];

            var isSetMatched = handlerConfiguration.get().sets().stream()
                .anyMatch(setConfiguration -> setConfiguration.identifierSetSpec().equals(set));

            if (!isSetMatched) {
                response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
                return null;
            }

            switch (set) {
                case "Publications", "Products", "Patents":
                    recordClass = ExportDocument.class;
                    converterClass = ExportDocumentConverter.class;
                    break;
                case "Persons":
                    recordClass = ExportPerson.class;
                    converterClass = ExportPersonConverter.class;
                    break;
                case "Projects":
                    // TODO: to be implemented
                    break;
                case "Events":
                    recordClass = ExportEvent.class;
                    converterClass = ExportEventConverter.class;
                    break;
                case "Orgunits":
                    recordClass = ExportOrganisationUnit.class;
                    converterClass = ExportOrganisationUnitConverter.class;
                    break;
                case "Funding":
                    // TODO: to be implemented
                    break;
                case "Equipments":
                    // TODO: to be implemented
                    break;
                default:
                    recordClass = ExportDocument.class; // Default case if none match
                    converterClass = ExportDocumentConverter.class;
                    break;
            }
        } else {
            set = "Publications";
            recordClass = ExportDocument.class;
            converterClass = ExportDocumentConverter.class;
        }

        if (Objects.isNull(recordClass)) {
            response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
            return null;
        }

        var requestedRecordOptional =
            findRequestedRecord(identifier, recordClass, handlerConfiguration.get());
        if (requestedRecordOptional.isEmpty()) {
            response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
            return null;
        }

        record.setHeader(constructOaiResponseHeader(handlerConfiguration.get(),
            (BaseExportEntity) requestedRecordOptional.get(), identifier, set));

        if (Objects.nonNull(record.getHeader().getStatus()) &&
            record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
            return getRecord;
        }

        setMetadataFieldsInGivenFormat(set, recordClass, converterClass, metadataFormat, metadata,
            requestedRecordOptional.get());

        record.setMetadata(metadata);
        return getRecord;
    }

    private Header constructOaiResponseHeader(
        ExportHandlersConfigurationLoader.Handler handlerConfig, BaseExportEntity exportEntity,
        String identifier, String identifierSetSpec) {
        var header = new Header();
        if (exportEntity.getDeleted()) {
            header.setStatus("deleted");
        }
        header.setIdentifier(identifier);
        header.setDatestamp(exportEntity.getLastUpdated()); // TODO: is this right

        handlerConfig.sets().forEach(setConfigs -> {
            if (setConfigs.identifierSetSpec().equals(identifierSetSpec) ||
                setConfigs.setSpec().equals(identifierSetSpec)) {
                header.setSetSpec(setConfigs.setSpec());
            }
        });

        return header;
    }

    private <E> void setMetadataFieldsInGivenFormat(String set, Class<?> recordClass,
                                                    Class<?> converterClass,
                                                    ExportDataFormat metadataFormat,
                                                    Metadata metadata, E requestedRecord) {
        var conversionFunctionName = switch (metadataFormat) {
            case OAI_CERIF_OPENAIRE -> "toOpenaireModel";
            case DUBLIN_CORE -> "toDCModel";
        };

        try {
            var conversionMethod = converterClass.getMethod(conversionFunctionName, recordClass);
            Object convertedEntity = conversionMethod.invoke(null, requestedRecord);

            switch (set) {
                case "Publications":
                case "Products":
                case "Patents":
                    metadata.setPublication((AbstractPublication) convertedEntity);
                    break;
                case "Persons":
                    metadata.setPerson((Person) convertedEntity);
                    break;
                case "Events":
                    metadata.setEvent((Event) convertedEntity);
                    break;
                case "Orgunits":
                    metadata.setOrgUnit((OrgUnit) convertedEntity);
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("Conversion method invocation failed", e);
        }
    }

    private <E> Optional<E> findRequestedRecord(String identifier, Class<E> entityClass,
                                                ExportHandlersConfigurationLoader.Handler handlerConfiguration) {
        Query query = new Query();

        if (identifier.contains("BISIS")) {
            query.addCriteria(
                Criteria.where("oldId").is(OAIPMHParseUtility.parseBISISID(identifier)));
        } else if (identifier.contains("TESLARIS")) {
            query.addCriteria(
                Criteria.where("databaseId").is(OAIPMHParseUtility.parseBISISID(identifier)));
        }

        if (handlerConfiguration.exportOnlyActiveEmployees()) {
            query.addCriteria(Criteria.where("activelyRelatedInstitutionIds")
                .in(Integer.parseInt(handlerConfiguration.internalInstitutionId())));
        } else {
            query.addCriteria(Criteria.where("relatedInstitutionIds")
                .in(Integer.parseInt(handlerConfiguration.internalInstitutionId())));
        }
        query.limit(1);

        return Optional.ofNullable(mongoTemplate.findOne(query, entityClass));
    }

    private <E> List<E> findRequestedRecords(Class<E> entityClass, String from, String until,
                                             int page,
                                             ExportHandlersConfigurationLoader.Handler handlerConfiguration) {
        Query query = new Query();

        query.addCriteria(Criteria.where("last_updated").gte(Date.valueOf(
                LocalDate.parse(from, DateTimeFormatter.ISO_DATE)))
            .lte(Date.valueOf(
                LocalDate.parse(until, DateTimeFormatter.ISO_DATE))));

        if (handlerConfiguration.exportOnlyActiveEmployees()) {
            query.addCriteria(Criteria.where("activelyRelatedInstitutionIds")
                .in(Integer.parseInt(handlerConfiguration.internalInstitutionId())));
        } else {
            query.addCriteria(Criteria.where("relatedInstitutionIds")
                .in(Integer.parseInt(handlerConfiguration.internalInstitutionId())));
        }

        query.with(PageRequest.of(page, PAGE_SIZE));
        return mongoTemplate.find(query, entityClass);
    }

    @Override
    public Identify identifyHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var identify = new Identify();
        identify.setBaseURL(baseUrl + "/" + handler);
        identify.setRepositoryName(repositoryName);
        identify.setProtocolVersion("2.0");
        identify.setAdminEmail("mailto:" + adminEmail);

        var earliestDocument = findEarliestDocument(
            Integer.parseInt(handlerConfiguration.get().internalInstitutionId()));
        earliestDocument.ifPresent(
            exportDocument -> identify.setEarliestDatestamp(exportDocument.getDocumentDate()));

        identify.setDeletedRecord("persistent");
        identify.setGranularity("YYYY-MM-DD");
        identify.setCompression(List.of("gzip", "deflate"));

        var service = getServiceDescription(identify, handlerConfiguration.get());

        var oaiIdentifier = new OAIIdentifier();
        oaiIdentifier.setScheme("oai");
        oaiIdentifier.setRepositoryIdentifier(repositoryName);
        oaiIdentifier.setDelimiter(":");
        oaiIdentifier.setSampleIdentifier("oai:CRIS.UNS:Publications/(BISIS)1000");

        var toolkit = new Toolkit(); // TODO: Check this data
        toolkit.setTitle("Sci2Zero Alliance Custom implementation");
        toolkit.setAuthor(
            new Toolkit.Author("Sci2Zero team", "mailto:chenejac@uns.ac.rs", "Sci2Zero"));
        toolkit.setVersion("1.0.0");

        var serviceDescription = new Description();
        serviceDescription.setService(service);
        var identifierDescription = new Description();
        identifierDescription.setOaiIdentifier(oaiIdentifier);
        var toolkitDescription = new Description();
        toolkitDescription.setToolkit(toolkit);

        identify.getDescriptions()
            .addAll(List.of(serviceDescription, identifierDescription, toolkitDescription));

        return identify;
    }

    private Optional<ExportDocument> findEarliestDocument(int internalInstitutionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("relatedInstitutionIds").in(internalInstitutionId));
        query.with(Sort.by(Sort.Direction.ASC, "documentDate"));
        query.limit(1);

        ExportDocument earliestDocument = mongoTemplate.findOne(query, ExportDocument.class);
        return Optional.ofNullable(earliestDocument);
    }

    @NotNull
    private ServiceDescription getServiceDescription(Identify identify,
                                                     ExportHandlersConfigurationLoader.Handler handler) {
        var service = new ServiceDescription();
        service.setCompatibility(
            "https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Service_Compatibility#1.1");
        service.setAcronym(repositoryName);
        service.setName(handler.handlerName());
        service.setDescription(handler.handlerDescription());
        service.setWebsiteURL(frontendURL);
        service.setOaiPMHBaseURL(identify.getBaseURL());
        return service;
    }

    @Override
    public ListSets listSetsForHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var listSets = new ListSets();
        handlerConfiguration.get().sets().forEach(set -> {
            var newSet = new Set();
            newSet.setSetName(set.setName());
            newSet.setSetSpec(set.setSpec());
            listSets.getSet().add(newSet);
        });

        return listSets;
    }

    @Override
    public ListMetadataFormats listMetadataFormatsForHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var listMetadataFormats = new ListMetadataFormats();
        handlerConfiguration.get().metadataFormats().forEach(format -> {
            var newMetadataFormat = new MetadataFormat();
            newMetadataFormat.setMetadataPrefix(format);
            setCommonMetadataFormatFields(format, newMetadataFormat);
            listMetadataFormats.getMetadataFormats().add(newMetadataFormat);
        });

        return listMetadataFormats;
    }

    private void setCommonMetadataFormatFields(String format, MetadataFormat metadataFormat) {
        var exportFormatEnum = ExportDataFormat.fromStringValue(format);
        metadataFormat.setSchema(exportFormatEnum.getSchema());
        metadataFormat.setMetadataNamespace(exportFormatEnum.getNamespace());
    }

    private ResumptionToken constructResumptionToken(String from, String until, int page,
                                                     String set, String format) {
        return new ResumptionToken(from + "!" + until + "!" + set + "!" + (page + 1) + "!" + format,
            null,
            "" + page * PAGE_SIZE); // When to expire
    }
}
