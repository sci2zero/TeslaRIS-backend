package rs.teslaris.importer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.skgif.researchproduct.ResearchProduct;
import rs.teslaris.core.model.skgif.venue.Venue;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.deduplication.DeduplicationUtil;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.exporter.model.skgif.SKGIFListResponse;
import rs.teslaris.importer.model.common.DocumentImport;
import rs.teslaris.importer.model.common.PersonDocumentContribution;
import rs.teslaris.importer.service.interfaces.SKGIFHarvester;
import rs.teslaris.importer.utility.CommonHarvestUtility;
import rs.teslaris.importer.utility.CommonImportUtility;
import rs.teslaris.importer.utility.DeepObjectMerger;
import rs.teslaris.importer.utility.HarvestProgressReport;
import rs.teslaris.importer.utility.skgif.SKGIFHarvestConfigurationLoader;

@Service
@RequiredArgsConstructor
@Slf4j
public class SKGIFHarvesterImpl implements SKGIFHarvester {

    private static final int MAX_RESTART_NUMBER = 2;

    private final int PAGE_SIZE = 100;

    private final MongoTemplate mongoTemplate;

    private final RestTemplateProvider restTemplateProvider;

    private final PersonService personService;

    private final PersonIndexRepository personIndexRepository;


    @Override
    public void harvest(String sourceName, String authorIdentifier, String institutionIdentifier,
                        LocalDate startDate, LocalDate endDate, Integer userId) {
        var sourceConfiguration = getSourceConfiguration(sourceName);

        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var identifierFilter =
            constructIdentifierFilter(authorIdentifier, institutionIdentifier, objectMapper,
                sourceConfiguration);
        if (Objects.isNull(identifierFilter)) {
            return;
        }

        var adminUserIds = CommonImportUtility.getAdminUserIds();

        var identifyingDataset = constructIdentifyingDatasetName(sourceConfiguration);
        var harvestProgressReport = getProgressReport(identifyingDataset, userId);

        var page = 0;
        var pageSize = PAGE_SIZE;

        if (Objects.nonNull(harvestProgressReport)) {
            var pageAndPageSize = harvestProgressReport.getResumptionToken().split(":");
            page = Integer.parseInt(pageAndPageSize[0]);
            pageSize = Integer.parseInt(pageAndPageSize[1]);
        }

        var endpoint =
            constructSKGIFEndpoint(sourceConfiguration, startDate, endDate, identifierFilter);
        var restTemplate = restTemplateProvider.provideRestTemplate();

        var newEntriesCount = new HashMap<Integer, Integer>();

        var restartCount = 0;
        while (restartCount <= MAX_RESTART_NUMBER) {
            var shouldContinue = true;
            try {
                while (shouldContinue) {
                    String paginatedUrl = endpoint + "&page=" + page + "&page_size=" + pageSize;
                    ResponseEntity<String> responseEntity =
                        restTemplate.getForEntity(paginatedUrl, String.class);

                    if (responseEntity.getStatusCode() != HttpStatus.OK) {
                        break;
                    }

                    var results =
                        objectMapper.readValue(responseEntity.getBody(),
                            new TypeReference<SKGIFListResponse<ResearchProduct>>() {
                            });

                    if (Objects.nonNull(results.getGraph())) {
                        processParsedRecords(results.getGraph(), sourceConfiguration,
                            newEntriesCount,
                            userId, adminUserIds);
                    }

                    shouldContinue = results.getGraph().size() == PAGE_SIZE;
                }

                break;
            } catch (HttpClientErrorException e) {
                log.error("HTTP error for SKG-IF client ID {}: {}", identifierFilter,
                    e.getMessage());
                restartCount++;
            } catch (JsonProcessingException e) {
                log.error("JSON parsing error for SKG-IF client ID {}: {}", identifierFilter,
                    e.getMessage());
                break;
            } catch (ResourceAccessException e) {
                log.error("SKG-IF server is unreachable for ID {}: {}", identifierFilter,
                    e.getMessage());
                restartCount++;
            }
        }
    }

    @Override
    public List<String> getSources() {
        return SKGIFHarvestConfigurationLoader.getAllSourceNames();
    }

    private SKGIFHarvestConfigurationLoader.Source getSourceConfiguration(String sourceName) {
        var config = SKGIFHarvestConfigurationLoader.getSourceConfigurationByName(sourceName);
        if (Objects.isNull(config)) {
            throw new NotFoundException("SKG-IF source with given name does not exist.");
        }
        return config;
    }

    private String constructSKGIFEndpoint(SKGIFHarvestConfigurationLoader.Source sourceConfig,
                                          LocalDate from, LocalDate until,
                                          String identifierFilter) {
        return sourceConfig.baseUrl() + "/product?" + sourceConfig.metadataFormatParameter() +
            "=json" + "&" + sourceConfig.dateFromFilterParam() + "=" + from.toString() + "&" +
            sourceConfig.dateToFilterParam() + "=" + until.toString() +
            "&filter=" + sourceConfig.additionalFilters() +
            (StringUtil.valueExists(sourceConfig.additionalFilters()) ? "," : "") +
            identifierFilter;
    }

    private String constructIdentifyingDatasetName(
        SKGIFHarvestConfigurationLoader.Source sourceConfig) {
        return "HARVEST_" +
            sourceConfig.sourceName().replace(" ", "_") + "_SKG-IF-PRODUCTS";
    }

    @Nullable
    private HarvestProgressReport getProgressReport(String datasetName, Integer userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("dataset").is(datasetName))
            .addCriteria(Criteria.where("userId").is(userId));
        return mongoTemplate.findOne(query, HarvestProgressReport.class);
    }

    private void processParsedRecords(
        List<ResearchProduct> records,
        SKGIFHarvestConfigurationLoader.Source sourceConfiguration,
        Map<Integer, Integer> newEntriesCount,
        Integer userId,
        Set<Integer> adminUserIds
    ) {
        try {
            Class<?> converterClass = Class.forName(
                "rs.teslaris.importer.model.converter.harvest." +
                    sourceConfiguration.converterClass());

            var method =
                converterClass.getMethod("toCommonImportModel", ResearchProduct.class, String.class,
                    String.class);

            for (var record : records) {
                processSingleRecord(sourceConfiguration, record, method,
                    newEntriesCount, userId, adminUserIds);
            }
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            log.error("SERIOUS: Invalid converter ({}) specified in SKG-IF harvest.",
                sourceConfiguration.converterClass());
        }
    }

    private void processSingleRecord(
        SKGIFHarvestConfigurationLoader.Source sourceConfiguration,
        ResearchProduct publication,
        Method method,
        Map<Integer, Integer> newEntriesCount,
        Integer userId,
        Set<Integer> adminUserIds
    ) {
        try {
            ((Optional<DocumentImport>) method.invoke(null, publication,
                sourceConfiguration.sourceIdentifierPrefix(), sourceConfiguration.baseUrl()))
                .ifPresent(documentImport -> {
                    bindImportUsersForAll(documentImport);
                    if (Objects.isNull(documentImport.getImportUsersId()) ||
                        documentImport.getImportUsersId().isEmpty()) {
                        return;
                    }

                    var existingImport =
                        CommonImportUtility.findExistingImport(documentImport.getIdentifier());
                    if (Objects.isNull(existingImport)) {
                        existingImport =
                            CommonImportUtility.findImportByDOIOrMetadata(documentImport);
                        if (Objects.nonNull(existingImport)) {
                            // Probably imported before from other sources, which have higher priorities
                            // perform metadata enrichment, if possible
                            DeepObjectMerger.deepMerge(existingImport, documentImport);
                            mongoTemplate.save(existingImport, "documentImports");
                            return;
                        }
                    }

                    var embedding = CommonImportUtility.generateEmbedding(documentImport);
                    if (DeduplicationUtil.isDuplicate(existingImport, embedding, documentImport)) {
                        return;
                    }

                    if (Objects.nonNull(embedding)) {
                        documentImport.setEmbedding(DeduplicationUtil.toDoubleList(embedding));
                    }

                    documentImport.getImportUsersId().addAll(adminUserIds);
                    newEntriesCount.merge(userId, 1, Integer::sum);

                    CommonHarvestUtility.updateContributorEntryCount(documentImport,
                        documentImport.getContributions().stream()
                            .map(c -> c.getPerson().getOrcid()).toList(),
                        newEntriesCount,
                        personService);

                    mongoTemplate.save(documentImport, "documentImports");
                });
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("SERIOUS: Invalid converter invocation.", e);
        }
    }

    private void bindImportUsersForAll(DocumentImport documentImport) {
        documentImport.getContributions().forEach(authorship -> {
            if (Objects.nonNull(authorship.getPerson().getOrcid()) &&
                !authorship.getPerson().getOrcid().isBlank()) {
                var researcher =
                    personService.findUserByIdentifier(authorship.getPerson().getOrcid());
                if (researcher.isPresent()) {
                    documentImport.getImportUsersId().add(researcher.get().getId());
                    personIndexRepository.findByDatabaseId(
                            personService.getPersonIdForUserId(researcher.get().getId()))
                        .ifPresent(personIndex -> {
                            documentImport.getImportInstitutionsId()
                                .addAll(personIndex.getEmploymentInstitutionsIdHierarchy());
                        });
                } else {
                    bindImportUsers(documentImport, authorship);
                }
            } else {
                bindImportUsers(documentImport, authorship);
            }
        });
    }

    private void bindImportUsers(DocumentImport documentImport,
                                 PersonDocumentContribution authorship) {
        personService.findPeopleByNameAndEmployment(Arrays.stream(
                    authorship.getPerson().getName().toString().split(" "))
                .toList(), Pageable.unpaged(), false, null, false)
            .forEach(person -> {
                if (Objects.nonNull(person.getUserId()) && person.getUserId() > 0) {
                    documentImport.getImportUsersId().add(person.getUserId());
                }

                documentImport.getImportInstitutionsId()
                    .addAll(person.getEmploymentInstitutionsIdHierarchy());
            });
    }

    private String constructIdentifierFilter(String authorIdentifier, String institutionIdentifier,
                                             ObjectMapper objectMapper,
                                             SKGIFHarvestConfigurationLoader.Source sourceConfiguration) {
        String identifierFilter = "";
        if (StringUtil.valueExists(authorIdentifier)) {
            identifierFilter = "contributions.by.identifiers.value:" +
                StringUtil.normalizeIdentifier(authorIdentifier);
        } else if (StringUtil.valueExists(institutionIdentifier)) {
            String fetchUrl =
                sourceConfiguration.baseUrl() + "/organisation?filter=identifiers.value:" +
                    StringUtil.normalizeIdentifier(institutionIdentifier);
            ResponseEntity<String> responseEntity =
                restTemplateProvider.provideRestTemplate().getForEntity(fetchUrl, String.class);
            try {
                SKGIFListResponse<?> result = objectMapper.readValue(
                    responseEntity.getBody(),
                    objectMapper.getTypeFactory()
                        .constructParametricType(SKGIFListResponse.class, Venue.class)
                );

                if (result.getGraph().isEmpty()) {
                    return null;
                }

                identifierFilter = "relevant_organisations:" +
                    ((Venue) result.getGraph().getFirst()).getLocalIdentifier();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return identifierFilter;
    }
}
