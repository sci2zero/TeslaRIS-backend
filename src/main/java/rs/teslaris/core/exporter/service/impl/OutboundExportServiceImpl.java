package rs.teslaris.core.exporter.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exporter.model.common.BaseExportEntity;
import rs.teslaris.core.exporter.model.common.ExportDocument;
import rs.teslaris.core.exporter.model.common.ExportPublicationType;
import rs.teslaris.core.exporter.model.converter.ExportConverterBase;
import rs.teslaris.core.exporter.model.converter.ExportDocumentConverter;
import rs.teslaris.core.exporter.service.interfaces.OutboundExportService;
import rs.teslaris.core.exporter.util.ExportDataFormat;
import rs.teslaris.core.exporter.util.ExportHandlersConfigurationLoader;
import rs.teslaris.core.exporter.util.OAIErrorFactory;
import rs.teslaris.core.exporter.util.ResumptionTokenStash;
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
import rs.teslaris.core.importer.model.oaipmh.event.EventConvertable;
import rs.teslaris.core.importer.model.oaipmh.organisationunit.OrgUnitConvertable;
import rs.teslaris.core.importer.model.oaipmh.patent.PatentConvertable;
import rs.teslaris.core.importer.model.oaipmh.person.PersonConvertable;
import rs.teslaris.core.importer.model.oaipmh.product.ProductConvertable;
import rs.teslaris.core.importer.model.oaipmh.publication.PublicationConvertable;
import rs.teslaris.core.importer.utility.OAIPMHParseUtility;
import rs.teslaris.core.util.exceptionhandling.exception.ConverterDoesNotExistException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;

@Service
@RequiredArgsConstructor
public class OutboundExportServiceImpl implements OutboundExportService {

    private final MongoTemplate mongoTemplate;

    private final int PAGE_SIZE = 10;

    private final String EXPORT_ENTITY_BASE_PACKAGE = "rs.teslaris.core.exporter.model.common.";

    private final String EXPORT_CONVERTER_BASE_PACKAGE =
        "rs.teslaris.core.exporter.model.converter.";

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
                                            OAIPMHResponse response, int page,
                                            boolean identifiersOnly) {
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

        Class<?> recordClass;
        Class<?> converterClass;

        try {
            recordClass =
                Class.forName(EXPORT_ENTITY_BASE_PACKAGE + matchedSet.get().commonEntityClass());
            converterClass = Class.forName(
                EXPORT_CONVERTER_BASE_PACKAGE +
                    (Objects.nonNull(matchedSet.get().converterClass()) ?
                        matchedSet.get().converterClass() :
                        (matchedSet.get().commonEntityClass() + "Converter")));
        } catch (ClassNotFoundException e) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

        var listRecords = new ListRecords();
        listRecords.setRecords(new ArrayList<>());

        var publicationTypeFilters = new ArrayList<ExportPublicationType>();
        if (Objects.nonNull(matchedSet.get().publicationTypes())) {
            var stringTypes = matchedSet.get().publicationTypes().split(",");
            Arrays.stream(stringTypes).forEach(stringType -> {
                publicationTypeFilters.add(ExportPublicationType.fromStringValue(stringType));
            });
        }

        var recordsPage =
            findRequestedRecords(recordClass, from, until, page, handlerConfiguration.get(),
                publicationTypeFilters);

        if (recordsPage.getTotalElements() == 0) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

        for (var fetchedRecordEntity : recordsPage.getContent()) {
            var record = new Record();
            listRecords.getRecords().add(record);
            var metadata = new Metadata();

            record.setHeader(constructOaiResponseHeader(handlerConfiguration.get(),
                (BaseExportEntity) fetchedRecordEntity, ("oai:" + repositoryName + ":") +
                    (!matchedSet.get().identifierSetSpec().isBlank() ?
                        (matchedSet.get().identifierSetSpec() + "/") : "") + "(TESLARIS)" +
                    ((BaseExportEntity) fetchedRecordEntity).getDatabaseId(),
                matchedSet.get().identifierSetSpec()));

            if (Objects.nonNull(record.getHeader().getStatus()) &&
                record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
                return listRecords;
            }

            if (!identifiersOnly) {
                try {
                    setMetadataFieldsInGivenFormat(matchedSet.get().identifierSetSpec(),
                        recordClass,
                        converterClass, ExportDataFormat.fromStringValue(metadataPrefix), metadata,
                        fetchedRecordEntity);
                } catch (ConverterDoesNotExistException e) {
                    response.setError(OAIErrorFactory.constructNoRecordsMatchError());
                    return null;
                }
                record.setMetadata(metadata);
            }
        }

        if (!recordsPage.isLast()) {
            listRecords.setResumptionToken(
                constructResumptionToken(from, until, page, requestedSet, metadataPrefix,
                    recordsPage.getTotalElements(), handlerConfiguration.get()));
        }

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

        Class<?> recordClass;
        Class<?> converterClass;

        String set;
        if (identifier.contains("/")) {
            try {
                set = identifier.split("/")[0].split(":")[2];
            } catch (IndexOutOfBoundsException e) {
                response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
                return null;
            }

            var matchedSet = handlerConfiguration.get().sets().stream()
                .filter(configuredSet -> configuredSet.identifierSetSpec().equals(set))
                .findFirst();

            if (matchedSet.isEmpty() || matchedSet.get().commonEntityClass().equals("NONE")) {
                response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
                return null;
            }

            try {
                recordClass = Class.forName(
                    EXPORT_ENTITY_BASE_PACKAGE + matchedSet.get().commonEntityClass());
                converterClass = Class.forName(
                    EXPORT_CONVERTER_BASE_PACKAGE +
                        (Objects.nonNull(matchedSet.get().converterClass()) ?
                            matchedSet.get().converterClass() :
                            (matchedSet.get().commonEntityClass() + "Converter")));
            } catch (ClassNotFoundException e) {
                response.setError(OAIErrorFactory.constructNoRecordsMatchError());
                return null;
            }
        } else {
            set = "Publications";
            recordClass = ExportDocument.class;
            converterClass = ExportDocumentConverter.class;
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

        try {
            setMetadataFieldsInGivenFormat(set, recordClass, converterClass, metadataFormat,
                metadata,
                requestedRecordOptional.get());
        } catch (ConverterDoesNotExistException e) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

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
                                                    Metadata metadata, E requestedRecord)
        throws ConverterDoesNotExistException {
        var conversionFunctionName = switch (metadataFormat) {
            case OAI_CERIF_OPENAIRE -> "toOpenaireModel";
            case DUBLIN_CORE -> "toDCModel";
            case ETD_MS -> "toETDMSModel";
            case DSPACE_INTERNAL_MODEL -> "toDIMModel";
        };

        try {
            var conversionMethod = converterClass.getMethod(conversionFunctionName, recordClass);
            Object convertedEntity = conversionMethod.invoke(null, requestedRecord);

            // TODO: discuss this
            ExportConverterBase.performExceptionalHandlingWhereAbsolutelyNecessary(convertedEntity,
                metadataFormat, set);

            switch (set) {
                case "Publications":
                    metadata.setPublication((PublicationConvertable) convertedEntity);
                    break;
                case "Products":
                    metadata.setProduct((ProductConvertable) convertedEntity);
                    break;
                case "Patents":
                    metadata.setPatent((PatentConvertable) convertedEntity);
                    break;
                case "Persons":
                    metadata.setPerson((PersonConvertable) convertedEntity);
                    break;
                case "Events":
                    metadata.setEvent((EventConvertable) convertedEntity);
                    break;
                case "Orgunits":
                    metadata.setOrgUnit((OrgUnitConvertable) convertedEntity);
                    break;
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new ConverterDoesNotExistException(
                converterClass.getName() + "." + conversionFunctionName + " is not implemented.");
        }
    }

    private <E> Optional<E> findRequestedRecord(String identifier, Class<E> entityClass,
                                                ExportHandlersConfigurationLoader.Handler handlerConfiguration) {
        var query = new Query();

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

    private <E> Page<E> findRequestedRecords(Class<E> entityClass, String from, String until,
                                             int page,
                                             ExportHandlersConfigurationLoader.Handler handlerConfiguration,
                                             List<ExportPublicationType> publicationTypeFilters) {
        var query = new Query();

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

        if (!publicationTypeFilters.isEmpty()) {
            query.addCriteria(Criteria.where("type").in(publicationTypeFilters));
        }

        var totalCount = mongoTemplate.count(query, entityClass);

        var pageRequest = PageRequest.of(page, PAGE_SIZE);
        query.with(pageRequest);
        var records = mongoTemplate.find(query, entityClass);

        return new PageImpl<>(records, pageRequest, totalCount);
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
        identify.setAdminEmail(adminEmail);

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
        oaiIdentifier.setSampleIdentifier(
            "oai:" + repositoryName.replace(" ", ".") + ":Publications/(TESLARIS)1000");

        var toolkit = new Toolkit();
        toolkit.setTitle("Sci2Zero Alliance Custom implementation");
        toolkit.setAuthor(
            new Toolkit.Author("Sci2Zero team", "chenejac@uns.ac.rs", "Science 2.0 Alliance"));
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
                                                     String set, String format,
                                                     long completeListSize,
                                                     ExportHandlersConfigurationLoader.Handler handler) {
        var tokenId = UUID.randomUUID().toString();
        var newToken =
            new ResumptionToken(
                Strings.join(List.of(from, until, set, String.valueOf(page + 1), format, tokenId),
                    '!'),
                Date.from(
                    LocalDateTime.now()
                        .plus(Duration.ofMinutes((handler.tokenExpirationTimeMinutes())))
                        .atZone(ZoneId.systemDefault())
                        .toInstant()),
                page * PAGE_SIZE, completeListSize);

        mongoTemplate.save(
            new ResumptionTokenStash(null, newToken.getValue(), newToken.getExpirationDate()));
        mongoTemplate.indexOps(ResumptionTokenStash.class)
            .ensureIndex(new Index().on("expirationTimestamp", Sort.Direction.ASC)
                .expire(0L));
        return newToken;
    }
}
