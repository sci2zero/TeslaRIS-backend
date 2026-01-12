package rs.teslaris.exporter.service.impl;

import java.lang.reflect.InvocationTargetException;
import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.oaipmh.common.Description;
import rs.teslaris.core.model.oaipmh.common.GetRecord;
import rs.teslaris.core.model.oaipmh.common.Header;
import rs.teslaris.core.model.oaipmh.common.Identify;
import rs.teslaris.core.model.oaipmh.common.ListMetadataFormats;
import rs.teslaris.core.model.oaipmh.common.ListRecords;
import rs.teslaris.core.model.oaipmh.common.ListSets;
import rs.teslaris.core.model.oaipmh.common.Metadata;
import rs.teslaris.core.model.oaipmh.common.MetadataFormat;
import rs.teslaris.core.model.oaipmh.common.Name;
import rs.teslaris.core.model.oaipmh.common.OAIIdentifier;
import rs.teslaris.core.model.oaipmh.common.OAIPMHResponse;
import rs.teslaris.core.model.oaipmh.common.Record;
import rs.teslaris.core.model.oaipmh.common.ResumptionToken;
import rs.teslaris.core.model.oaipmh.common.ServiceDescription;
import rs.teslaris.core.model.oaipmh.common.ServiceDescriptionContent;
import rs.teslaris.core.model.oaipmh.common.Set;
import rs.teslaris.core.model.oaipmh.common.Toolkit;
import rs.teslaris.core.model.oaipmh.event.EventConvertable;
import rs.teslaris.core.model.oaipmh.organisationunit.OrgUnitConvertable;
import rs.teslaris.core.model.oaipmh.patent.PatentConvertable;
import rs.teslaris.core.model.oaipmh.person.PersonConvertable;
import rs.teslaris.core.model.oaipmh.product.ProductConvertable;
import rs.teslaris.core.model.oaipmh.publication.PublicationConvertable;
import rs.teslaris.core.repository.institution.OrganisationUnitsRelationRepository;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.ConverterDoesNotExistException;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.persistence.IdentifierUtil;
import rs.teslaris.core.util.search.CollectionOperations;
import rs.teslaris.exporter.model.common.BaseExportEntity;
import rs.teslaris.exporter.model.common.ExportDocument;
import rs.teslaris.exporter.model.common.ExportPublicationType;
import rs.teslaris.exporter.model.converter.ExportConverterBase;
import rs.teslaris.exporter.model.converter.ExportDocumentConverter;
import rs.teslaris.exporter.service.interfaces.OutboundExportService;
import rs.teslaris.exporter.util.ExportDataFormat;
import rs.teslaris.exporter.util.ExportHandlersConfigurationLoader;
import rs.teslaris.exporter.util.OAIErrorFactory;
import rs.teslaris.exporter.util.ResumptionTokenStash;
import rs.teslaris.importer.utility.oaipmh.OAIPMHParseUtility;

@Service
@RequiredArgsConstructor
@Traceable
public class OutboundExportServiceImpl implements OutboundExportService {

    private final MongoTemplate mongoTemplate;

    private final OrganisationUnitService organisationUnitService;

    private final OrganisationUnitsRelationRepository organisationUnitsRelationRepository;

    private final int PAGE_SIZE = 10;

    private final String EXPORT_ENTITY_BASE_PACKAGE = "rs.teslaris.exporter.model.common.";

    private final String EXPORT_CONVERTER_BASE_PACKAGE =
        "rs.teslaris.exporter.model.converter.";

    @Value("${export.base.url}")
    private String baseUrl;

    @Value("${export.repo.name}")
    private String repositoryName;

    @Value("${export.admin.email}")
    private String adminEmail;

    @Value("${frontend.application.address}")
    private String frontendUrl;


    @Override
    public ListRecords listRequestedRecords(String handler, String metadataPrefix,
                                            String from, String until, String requestedSet,
                                            OAIPMHResponse response, int page,
                                            boolean identifiersOnly) {
        if (Objects.isNull(metadataPrefix) || metadataPrefix.isBlank() ||
            Objects.isNull(from) || from.isBlank() ||
            Objects.isNull(until) || until.isBlank()) {
            response.setError(OAIErrorFactory.constructBadArgumentError());
            return null;
        }

        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty() || handlerConfiguration.get().sets().isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        if (handlerConfiguration.get().metadataFormats().stream()
            .noneMatch(format -> format.equals(metadataPrefix))) {
            response.setError(OAIErrorFactory.constructFormatError(metadataPrefix));
            return null;
        }

        if (Objects.isNull(requestedSet) || requestedSet.isBlank()) {
            requestedSet = handlerConfiguration.get().sets().stream()
                .filter(
                    set -> Objects.nonNull(set.isDefaultSet()) && set.isDefaultSet().equals(true))
                .findFirst().orElse(
                    handlerConfiguration.get().sets().getFirst()
                ).setSpec();
        }

        String finalRequestedSet = requestedSet;
        var matchedSet = handlerConfiguration.get().sets().stream()
            .filter(set -> set.setSpec().equals(finalRequestedSet))
            .findFirst();

        if (matchedSet.isEmpty() || Objects.isNull(matchedSet.get().commonEntityClass()) ||
            matchedSet.get().commonEntityClass().equals("NONE")) {
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
        if (identifiersOnly) {
            listRecords.setHeader(new ArrayList<>());
        } else {
            listRecords.setRecords(new ArrayList<>());
        }

        var publicationTypeFilters = new ArrayList<ExportPublicationType>();
        if (Objects.nonNull(matchedSet.get().publicationTypes())) {
            var stringTypes = matchedSet.get().publicationTypes().split(",");
            Arrays.stream(stringTypes).forEach(stringType -> {
                publicationTypeFilters.add(ExportPublicationType.fromStringValue(stringType));
            });
        }

        var concreteTypeFilters = new HashMap<String, List<String>>();
        if (Objects.nonNull(matchedSet.get().concreteTypeFilters())) {
            var filters = matchedSet.get().concreteTypeFilters().split(";");
            Arrays.stream(filters).forEach(filter -> {
                var filterParts = filter.split(":");
                var stringTypes = filterParts[1].split(",");
                concreteTypeFilters.put(filterParts[0], Arrays.stream(stringTypes).toList());
            });
        }

        var additionalFilters = new HashMap<String, String>();
        if (Objects.nonNull(matchedSet.get().additionalFilters())) {
            var filters = matchedSet.get().additionalFilters().split(";");
            Arrays.stream(filters).forEach(filter -> {
                var filterParts = filter.split("=", 2);
                additionalFilters.put(filterParts[0], filterParts[1]);
            });
        }

        var recordsPage =
            findRequestedRecords(recordClass, from, until, page, handlerConfiguration.get(),
                publicationTypeFilters, concreteTypeFilters, additionalFilters);

        if (recordsPage.getTotalElements() == 0) {
            response.setError(OAIErrorFactory.constructNoRecordsMatchError());
            return null;
        }

        for (var fetchedRecordEntity : recordsPage.getContent()) {
            var header = constructOaiResponseHeader(
                handlerConfiguration.get(),
                (BaseExportEntity) fetchedRecordEntity,
                constructRecordIdentifier(
                    handlerConfiguration.get(),
                    (BaseExportEntity) fetchedRecordEntity,
                    repositoryName,
                    matchedSet.get()
                ),
                matchedSet.get().identifierSetSpec()
            );

            if (identifiersOnly) {
                listRecords.getHeader().add(header);
            } else {
                var record = new Record();
                listRecords.getRecords().add(record);
                record.setHeader(header);

                if (Objects.nonNull(record.getHeader().getStatus()) &&
                    record.getHeader().getStatus().equalsIgnoreCase("deleted")) {
                    return listRecords;
                }

                var metadata = new Metadata();
                try {
                    setMetadataFieldsInGivenFormat(matchedSet.get().identifierSetSpec(),
                        recordClass,
                        converterClass, ExportDataFormat.fromStringValue(metadataPrefix), metadata,
                        fetchedRecordEntity, handlerConfiguration.get());
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
                set = identifier.split("/")[0];
                if (set.contains(":")) {
                    set = set.split(":")[2];
                }
            } catch (IndexOutOfBoundsException e) {
                response.setError(OAIErrorFactory.constructNotFoundOrForbiddenError(identifier));
                return null;
            }

            var parsedSetValue = set;
            var matchedSet = handlerConfiguration.get().sets().stream()
                .filter(configuredSet -> configuredSet.identifierSetSpec().equals(parsedSetValue))
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
                metadata, requestedRecordOptional.get(), handlerConfiguration.get());
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
        header.setDatestamp(DateTimeFormatter.ISO_INSTANT.format(
            exportEntity.getLastUpdated().toInstant()));

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
                                                    Metadata metadata, E requestedRecord,
                                                    ExportHandlersConfigurationLoader.Handler handler)
        throws ConverterDoesNotExistException {
        var conversionFunctionName = switch (metadataFormat) {
            case OAI_CERIF_OPENAIRE -> "toOpenaireModel";
            case DUBLIN_CORE -> "toDCModel";
            case ETD_MS -> "toETDMSModel";
            case DSPACE_INTERNAL_MODEL -> "toDIMModel";
            case MARC21 -> "toMARC21Model";
        };

        try {
            var conversionMethod =
                List.of("Publications", "Theses", "Products", "Patents").contains(set) ?
                    converterClass.getMethod(conversionFunctionName, recordClass,
                        boolean.class, List.class, Map.class) :
                    converterClass.getMethod(conversionFunctionName, recordClass,
                        boolean.class, List.class);

            boolean supportLegacyIdentifiers = handler.supportLegacyIdentifiers();
            List<String> supportedLanguages =
                CollectionOperations.containsValues(handler.supportedLanguages()) ?
                    handler.supportedLanguages().stream().map(String::toLowerCase).toList() :
                    Collections.emptyList();

            Object convertedEntity =
                conversionMethod.invoke(
                    null, requestedRecord, supportLegacyIdentifiers,
                    supportedLanguages, handler.getTypeToIdentifierSuffixMapping()
                );

            ExportConverterBase.applyCustomMappings(convertedEntity, metadataFormat,
                organisationUnitService, handler);

            ExportConverterBase.performExceptionalHandlingWhereAbsolutelyNecessary(convertedEntity,
                metadataFormat, recordClass, handler);

            switch (set) {
                case "Publications", "Theses":
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

        if (identifier.contains("_")) {
            var identifierParts = identifier.split("_", 2);
            identifier = identifierParts[0];
            var suffix = identifierParts[1];
            var requiredType =
                handlerConfiguration.getIdentifierSuffixToTypeMapping().getOrDefault(suffix, null);
            if (Objects.nonNull(requiredType)) {
                query.addCriteria(
                    Criteria.where("type")
                        .is(ExportPublicationType.fromStringValue(requiredType)));
            }
        } else {
            var excludedTypes = handlerConfiguration
                .getTypeToIdentifierSuffixMapping()
                .keySet()
                .stream()
                .map(ExportPublicationType::fromStringValue)
                .collect(Collectors.toList());

            query.addCriteria(
                Criteria.where("type").nin(excludedTypes)
            );
        }

        if (identifier.contains(IdentifierUtil.legacyIdentifierPrefix)) {
            query.addCriteria(
                Criteria.where("old_id").in(OAIPMHParseUtility.parseBISISID(identifier)));
        } else if (identifier.contains(IdentifierUtil.identifierPrefix)) {
            query.addCriteria(
                Criteria.where("database_id").is(OAIPMHParseUtility.parseBISISID(identifier)));
        } else {
            // TODO: We could also throw an error here or simply join with upper clause
            query.addCriteria(
                Criteria.where("database_id").is(OAIPMHParseUtility.parseBISISID(identifier)));
        }

        if (handlerConfiguration.exportOnlyActiveEmployees()) {
            query.addCriteria(Criteria.where("actively_related_institution_ids")
                .in(getAllOUSubUnitsIds(
                    Integer.parseInt(handlerConfiguration.internalInstitutionId()))));
        } else {
            query.addCriteria(Criteria.where("related_institution_ids")
                .in(getAllOUSubUnitsIds(
                    Integer.parseInt(handlerConfiguration.internalInstitutionId()))));
        }
        query.limit(1);

        return Optional.ofNullable(mongoTemplate.findOne(query, entityClass));
    }

    private <E> Page<E> findRequestedRecords(Class<E> entityClass, String from, String until,
                                             int page,
                                             ExportHandlersConfigurationLoader.Handler handlerConfiguration,
                                             List<ExportPublicationType> publicationTypeFilters,
                                             HashMap<String, List<String>> concreteTypeFilters,
                                             HashMap<String, String> additionalFilters) {
        var query = new Query();

        query.addCriteria(Criteria.where("last_updated").gte(Date.valueOf(
                LocalDate.parse(from, DateTimeFormatter.ISO_DATE)))
            .lte(Date.valueOf(
                LocalDate.parse(until, DateTimeFormatter.ISO_DATE))));

        if (handlerConfiguration.exportOnlyActiveEmployees()) {
            query.addCriteria(Criteria.where("actively_related_institution_ids")
                .in(getAllOUSubUnitsIds(
                    Integer.parseInt(handlerConfiguration.internalInstitutionId()))));
        } else {
            query.addCriteria(Criteria.where("related_institution_ids")
                .in(getAllOUSubUnitsIds(
                    Integer.parseInt(handlerConfiguration.internalInstitutionId()))));
        }

        if (!publicationTypeFilters.isEmpty()) {
            query.addCriteria(Criteria.where("type").in(publicationTypeFilters));
        }

        if (!additionalFilters.isEmpty()) {
            additionalFilters.forEach((field, value) -> {
                if (value.startsWith("bool:")) {
                    query.addCriteria(
                        Criteria.where(field).is(
                            Boolean.parseBoolean(value.replace("bool:", ""))
                        )
                    );
                } else {
                    query.addCriteria(Criteria.where(field).is(value));
                }
            });
        }

        List<Criteria> allFieldCriteria = new ArrayList<>();

        concreteTypeFilters.forEach((field, types) -> {
            Criteria fieldCriteria = new Criteria().orOperator(
                Criteria.where(field).in(types),
                Criteria.where(field).exists(false)
            );
            allFieldCriteria.add(fieldCriteria);
        });

        if (!allFieldCriteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(
                allFieldCriteria.toArray(new Criteria[0])
            ));
        }

        var totalCount = mongoTemplate.count(query, entityClass);

        var pageRequest = PageRequest.of(page, PAGE_SIZE);
        query.with(pageRequest);
        var records = mongoTemplate.find(query, entityClass);

        return new PageImpl<>(records, pageRequest, totalCount);
    }

    private HashSet<Integer> getAllOUSubUnitsIds(Integer organisationUnitId) {
        return new HashSet<>(
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId));
    }

    @Override
    public Identify identifyHandler(String handler) {
        var handlerConfiguration =
            ExportHandlersConfigurationLoader.getHandlerByIdentifier(handler);
        if (handlerConfiguration.isEmpty()) {
            throw new LoadingException("No handler with identifier " + handler);
        }

        var identify = new Identify();
        identify.setBaseURL(baseUrl + "/api/export/" + handler);
        identify.setRepositoryName(repositoryName);
        identify.setProtocolVersion("2.0");
        identify.setAdminEmail(adminEmail);

        var earliestDocument = findEarliestDocument(
            Integer.parseInt(handlerConfiguration.get().internalInstitutionId()));
        earliestDocument.ifPresent(
            exportDocument -> identify.setEarliestDatestamp(
                exportDocument.getLastUpdated().toInstant().atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate().toString()));

        identify.setDeletedRecord("persistent");
        identify.setGranularity("YYYY-MM-DD");
        identify.setCompression(List.of("gzip", "deflate"));

        var service = getServiceDescription(identify, handlerConfiguration.get());

        var oaiIdentifier = new OAIIdentifier();
        oaiIdentifier.setScheme("oai");
        oaiIdentifier.setRepositoryIdentifier(repositoryName.replace(" ", "."));
        oaiIdentifier.setDelimiter(":");
        oaiIdentifier.setSampleIdentifier(
            "oai:" + repositoryName.replace(" ", ".") + ":Publications/" +
                IdentifierUtil.identifierPrefix + "1000");

        var toolkit = new Toolkit();
        toolkit.setTitle("Sci2Zero Alliance Custom implementation");
        toolkit.setAuthor(
            new Toolkit.Author("Sci2Zero team", "info@sci2zero.org", "Science 2.0 Alliance"));
        toolkit.setVersion("1.1.0");

        var serviceDescription = new Description();
        serviceDescription.setService(service);
        var identifierDescription = new Description();
        identifierDescription.setOaiIdentifier(oaiIdentifier);
        var toolkitDescription = new Description();
        toolkitDescription.setToolkit(toolkit);

        identify.getDescription()
            .addAll(List.of(serviceDescription, identifierDescription, toolkitDescription));

        return identify;
    }

    private Optional<ExportDocument> findEarliestDocument(int internalInstitutionId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("relatedInstitutionIds").in(internalInstitutionId));
        query.with(Sort.by(Sort.Direction.ASC, "last_updated"));
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
        service.setAcronym(repositoryName.replace(" ", "."));
        service.setName(new Name(handler.handlerLanguage(), handler.handlerName()));
        service.setDescription(
            new ServiceDescriptionContent(handler.handlerLanguage(), handler.handlerDescription()));
        service.setWebsiteURL(frontendUrl);
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
            listMetadataFormats.getMetadataFormat().add(newMetadataFormat);
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

    private String constructRecordIdentifier(
        ExportHandlersConfigurationLoader.Handler handlerConfig,
        BaseExportEntity entity,
        String repositoryName,
        ExportHandlersConfigurationLoader.Set matchedSet) {
        String basePrefix = "oai:" + repositoryName.replace(" ", ".") + ":";

        String setSpecPrefix = !matchedSet.identifierSetSpec().isBlank()
            ? matchedSet.identifierSetSpec() + "/"
            : "";

        String identifierPrefix =
            (handlerConfig.supportLegacyIdentifiers() && !entity.getOldIds().isEmpty())
                ? IdentifierUtil.legacyIdentifierPrefix
                : IdentifierUtil.identifierPrefix;

        var entityId = getEntityIdentifier(handlerConfig, entity);

        var identifierTypeSuffix = "";
        if (entity instanceof ExportDocument) {
            identifierTypeSuffix = handlerConfig.getTypeToIdentifierSuffixMapping()
                .getOrDefault(((ExportDocument) entity).getType().name(), "");
        }

        return basePrefix + setSpecPrefix + identifierPrefix + entityId + identifierTypeSuffix;
    }

    private Integer getEntityIdentifier(ExportHandlersConfigurationLoader.Handler handlerConfig,
                                        BaseExportEntity entity) {
        if (handlerConfig.supportLegacyIdentifiers() && !entity.getOldIds().isEmpty()) {
            return entity.getOldIds().stream().findFirst().get();
        }
        return entity.getDatabaseId();
    }
}
