package rs.teslaris.assessment.service.impl.indicator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.service.impl.indicator.harvester.OpenCitationsCitationCountHarvester;
import rs.teslaris.assessment.service.impl.indicator.harvester.UnpaywallOpenAccessHarvester;
import rs.teslaris.assessment.service.impl.indicator.worker.ExternalIndicatorWorker;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorHarvestService;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.util.ExternalMappingConstraintType;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.applicationevent.HarvestExternalIndicatorsEvent;
import rs.teslaris.core.applicationevent.ReindexExternalIndicatorsEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.functional.FunctionalUtil;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.core.util.session.RestTemplateProvider;
import rs.teslaris.core.util.session.ScopusAuthenticationHelper;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalIndicatorHarvestServiceImpl implements ExternalIndicatorHarvestService {

    private final RestTemplateProvider restTemplateProvider;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonService personService;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final IndicatorService indicatorService;

    private final ScopusAuthenticationHelper scopusAuthenticationHelper;

    private final PersonIndexRepository personIndexRepository;

    private final Lock harvestLock = new ReentrantLock();

    private final int MAX_RETRY_COUNT = 1;

    private final int PROCESS_BATCH_SIZE = 100;

    private final List<EntityIndicatorSource> EXTERNAL_INDICATOR_SOURCES =
        List.of(EntityIndicatorSource.OPEN_ALEX,
            EntityIndicatorSource.OPEN_CITATIONS,
            EntityIndicatorSource.SCOPUS);

    private final OpenCitationsCitationCountHarvester openCitationsHarvester;

    private final UnpaywallOpenAccessHarvester unpaywallOpenAccessHarvester;

    private final ExternalIndicatorWorker externalIndicatorWorker;

    private Map<String, String> externalIndicatorMapping;

    private Map<String, Integer> harvestPeriodOffsets;

    private Map<String, Integer> rateLimits;

    @Value("${harvest-external-indicators.allowed}")
    private Boolean harvestAllowed;


    @Override
    public void performOUIndicatorDeduction() {
        var context = prepareInstitutionIndicatorDeductionContext();

        var indicatorsToSave = new HashSet<OrganisationUnitIndicator>();
        FunctionalUtil.forEachChunked(
            PageRequest.of(0, PROCESS_BATCH_SIZE,
                Sort.by(Sort.Direction.ASC, "databaseId")),
            organisationUnitIndexRepository::findAll,
            institutions -> institutions.forEach(
                institution ->
                    externalIndicatorWorker.performInstitutionDeduction(
                        context.sources, institution,
                        context.totalCitationsIndicator, context.totalOutputIndicator,
                        indicatorsToSave
                    )
            )
        );

        organisationUnitIndicatorRepository.saveAll(indicatorsToSave);
    }

    @Override
    @Async("taskExecutor")
    public void performIndicatorHavestForSinglePerson(Integer personId) {
        var person = personService.findOne(personId);
        var harvestContext = preparePersonIndicatorHarvestContext();

        performPersonHarvest(person, harvestContext);
    }

    @Override
    @Async("taskExecutor")
    public void performIndicatorDeductionForSingleInstitution(Integer organisationUnitId) {
        organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
            organisationUnitId).ifPresent(institution -> {
            var context = prepareInstitutionIndicatorDeductionContext();

            var indicatorsToSave = new HashSet<OrganisationUnitIndicator>();

            externalIndicatorWorker.performInstitutionDeduction(
                context.sources, institution,
                context.totalCitationsIndicator, context.totalOutputIndicator,
                indicatorsToSave);

            organisationUnitIndicatorRepository.saveAll(indicatorsToSave);
        });
    }

    @Override
    @Async("taskExecutor")
    public void harvestAllManually() {
        performIndicatorHarvest();
    }

    @Override
    public void performPersonIndicatorHarvest() {
        var harvestContext = preparePersonIndicatorHarvestContext();

        FunctionalUtil.forEachChunked(
            PageRequest.of(0, PROCESS_BATCH_SIZE),
            personService::findPersonsByLRUHarvest,
            people -> people.forEach(person -> {
                performPersonHarvest(person, harvestContext);
            })
        );
    }

    private HarvestContext preparePersonIndicatorHarvestContext() {
        refreshConfiguration();

        var totalCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalCitationCount", null));
        var yearlyCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("yearlyCitationCount", null));
        var totalOutputIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalPublicationCount", null));
        var hIndexIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("hIndex", null));

        var openAlexRateLimit = new AtomicInteger(rateLimits.getOrDefault("openAlex", 0));
        var scopusRateLimit = new AtomicInteger(rateLimits.getOrDefault("scopus", 0));
        var openCitationsRateLimit = new AtomicInteger(rateLimits.getOrDefault("openCitations", 0));
        var unpaywallRateLimit = new AtomicInteger(rateLimits.getOrDefault("unpaywall", 0));

        return new HarvestContext(
            totalCitationsIndicator,
            yearlyCitationsIndicator,
            totalOutputIndicator,
            hIndexIndicator,
            openAlexRateLimit,
            scopusRateLimit,
            openCitationsRateLimit,
            unpaywallRateLimit
        );
    }

    private DeductionContext prepareInstitutionIndicatorDeductionContext() {
        refreshConfiguration();

        var totalCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalCitationCount", null));
        var totalOutputIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalPublicationCount", null));

        return new DeductionContext(
            totalCitationsIndicator,
            totalOutputIndicator,
            EXTERNAL_INDICATOR_SOURCES
        );
    }

    private void performPersonHarvest(Person person, HarvestContext context) {
        performPersonHarvest(person,
            context.totalCitationsIndicator(),
            context.yearlyCitationsIndicator(),
            context.totalOutputIndicator(),
            context.hIndexIndicator(),
            context.openAlexRateLimit(),
            context.scopusRateLimit(),
            context.openCitationsRateLimit(),
            context.unpaywallRateLimit()
        );
    }

    private void performPersonHarvest(Person person, Indicator totalCitationsIndicator,
                                      Indicator yearlyCitationsIndicator,
                                      Indicator totalOutputIndicator, Indicator hIndexIndicator,
                                      AtomicInteger openAlexRateLimit,
                                      AtomicInteger scopusRateLimit,
                                      AtomicInteger openCitationsRateLimit,
                                      AtomicInteger unpaywallRateLimit) {
        person.setDateOfLastIndicatorHarvest(LocalDate.now());
        personService.save(person);

        if (openAlexRateLimit.getAndDecrement() > 0) {
            harvestFromOpenAlex(
                person,
                totalCitationsIndicator,
                yearlyCitationsIndicator,
                totalOutputIndicator,
                hIndexIndicator
            );
        }
        if (Objects.nonNull(person.getScopusAuthorId()) &&
            !person.getScopusAuthorId().isBlank() &&
            scopusRateLimit.getAndDecrement() > 0) {
            harvestFromScopus(
                person,
                totalCitationsIndicator,
                yearlyCitationsIndicator,
                totalOutputIndicator,
                hIndexIndicator
            );
        }

        List<DocumentMetricHarvester> documentCentricHarvesters = new ArrayList<>();
        if (openCitationsRateLimit.getAndDecrement() > 0) {
            documentCentricHarvesters.add(openCitationsHarvester);
        }
        if (unpaywallRateLimit.getAndDecrement() > 0) {
            documentCentricHarvesters.add(unpaywallOpenAccessHarvester);
        }

        harvestFromDocumentCentricSources(
            person, documentCentricHarvesters,
            totalCitationsIndicator,
            yearlyCitationsIndicator,
            totalOutputIndicator,
            hIndexIndicator
        );
    }

    private void refreshConfiguration() {
        externalIndicatorMapping =
            IndicatorMappingConfigurationLoader.fetchExternalIndicatorMappings();
        harvestPeriodOffsets = IndicatorMappingConfigurationLoader.fetchExternalMappingConstraints(
            ExternalMappingConstraintType.HARVEST_PERIOD_OFFSET);
        rateLimits = IndicatorMappingConfigurationLoader.fetchExternalMappingConstraints(
            ExternalMappingConstraintType.RATE_LIMIT);
    }

    private void harvestFromOpenAlex(Person person, Indicator totalCitationsIndicator,
                                     Indicator yearlyCitationsIndicator,
                                     Indicator totalOutputIndicator, Indicator hIndexIndicator) {
        var harvestPeriodOffset = harvestPeriodOffsets.get("openAlex");
        var endDate = LocalDate.now();
        var startDate = endDate.minusYears(harvestPeriodOffset);

        var restTemplate = restTemplateProvider.provideRestTemplate();

        if (!StringUtil.valueExists(person.getOpenAlexId()) ||
            !StringUtil.valueExists(person.getOrcid()) ||
            !StringUtil.valueExists(person.getScopusAuthorId())) {
            performDataEnrichment(person);
        }

        var filter = constructAdequateOpenAlexSearchFilter(person);
        if (Objects.isNull(filter)) {
            return;
        }

        var baseUrl = "https://api.openalex.org/works?per-page=100" + "&filter=" + filter +
            ",from_publication_date:" + startDate + ",to_publication_date:" + endDate;

        var cursor = "*";
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var documentIndicators = new HashSet<DocumentIndicator>();
        try {
            HashMap<String, Integer> personAggregatedCounts = new HashMap<>();
            List<Integer> allCitationCounts = new ArrayList<>();
            int totalPublications = 0;

            while (Objects.nonNull(cursor)) {
                String paginatedUrl = baseUrl + "&cursor=" + cursor;
                ResponseEntity<String> responseEntity =
                    restTemplate.getForEntity(paginatedUrl, String.class);

                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    break;
                }

                var results =
                    objectMapper.readValue(responseEntity.getBody(), OpenAlexResults.class);

                if (Objects.nonNull(results.citationCounts()) &&
                    !results.citationCounts.isEmpty()) {
                    updateDocumentCitationCounts(results);

                    var citationCounts = results.citationCounts.stream()
                        .filter(citationResult -> citationResult.citationCount > 0).toList();
                    totalPublications += results.citationCounts.size();

                    personAggregatedCounts =
                        accumulateCitationCounts(citationCounts, personAggregatedCounts);

                    citationCounts.forEach(citationCount -> {
                        allCitationCounts.add(citationCount.citationCount);

                        documentPublicationService.findDocumentByCommonIdentifier(citationCount.doi,
                                citationCount.id, null, null)
                            .ifPresent(document -> {
                                if (Objects.isNull(totalCitationsIndicator)) {
                                    return;
                                }

                                var newCitationCountIndicator =
                                    documentIndicatorRepository.findIndicatorForCodeAndSourceDocumentId(
                                            totalCitationsIndicator.getCode(),
                                            EntityIndicatorSource.OPEN_ALEX, document.getId())
                                        .orElse(new DocumentIndicator());

                                newCitationCountIndicator.setDocument(document);
                                newCitationCountIndicator.setNumericValue(
                                    (double) citationCount.citationCount);
                                newCitationCountIndicator.setIndicator(totalCitationsIndicator);
                                newCitationCountIndicator.setSource(
                                    EntityIndicatorSource.OPEN_ALEX);
                                newCitationCountIndicator.setToDate(LocalDate.now());
                                documentIndicators.add(newCitationCountIndicator);
                            });
                    });
                }

                cursor = Objects.nonNull(results.meta()) ? results.meta().nextCursor() : null;
            }

            externalIndicatorWorker.persistPersonCitationIndicators(
                person, personAggregatedCounts, totalPublications,
                allCitationCounts, totalCitationsIndicator, yearlyCitationsIndicator,
                totalOutputIndicator, hIndexIndicator, EntityIndicatorSource.OPEN_ALEX);

            documentIndicatorRepository.saveAll(documentIndicators);
        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching OpenAlex works: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Exception occurred during connection to OpenAlex: {}", e.getMessage());
        }
    }

    private void harvestFromDocumentCentricSources(Person person,
                                                   List<DocumentMetricHarvester> harvesters,
                                                   Indicator totalCitationsIndicator,
                                                   Indicator yearlyCitationsIndicator,
                                                   Indicator totalOutputIndicator,
                                                   Indicator hIndexIndicator) {
        var metrics = harvestDocumentMetrics(
            person, harvesters
        );

        var citationCounts =
            metrics.getOrDefault(MetricType.CITATION_COUNT, Collections.emptyList());
        if (citationCounts.isEmpty()) {
            return;
        }

        externalIndicatorWorker.persistPersonCitationIndicators(
            person,
            new HashMap<>(
                Map.of(
                    "TOTAL",
                    citationCounts.stream()
                        .mapToInt(Integer::intValue)
                        .sum()
                )
            ),
            citationCounts.size(), citationCounts, totalCitationsIndicator,
            yearlyCitationsIndicator, totalOutputIndicator,
            hIndexIndicator, EntityIndicatorSource.OPEN_CITATIONS
        );
    }

    private void harvestFromScopus(Person person,
                                   Indicator totalCitationsIndicator,
                                   Indicator yearlyCitationsIndicator,
                                   Indicator totalOutputIndicator,
                                   Indicator hIndexIndicator) {
        if (scopusAuthenticationHelper.authenticate()) {
            var restTemplate = scopusAuthenticationHelper.restTemplate;
            var objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<Integer> allCitationCounts = new ArrayList<>();
            int totalPublications = 0;

            var requestHeaders = new HttpHeaders();
            scopusAuthenticationHelper.headers.forEach(requestHeaders::add);

            var harvestPeriodOffset = harvestPeriodOffsets.get("scopus");
            var endYear = LocalDate.now().getYear();
            var startYear = endYear - harvestPeriodOffset;

            var documentIndicators = new HashSet<DocumentIndicator>();

            ResponseEntity<String> responseEntity;
            var shouldRetry = true;
            var retryCount = 0;
            while (shouldRetry) {
                try {
                    var cursor = "*";
                    while (Objects.nonNull(cursor)) {
                        var url =
                            "https://api.elsevier.com/content/search/scopus?query=AU-ID(" +
                                person.getScopusAuthorId() +
                                ")&date=" + startYear + "-" + endYear +
                                "&count=100&view=STANDARD&cursor=" + cursor;

                        responseEntity =
                            restTemplate.exchange(url, HttpMethod.GET,
                                new HttpEntity<>(requestHeaders),
                                String.class);
                        var results =
                            objectMapper.readValue(responseEntity.getBody(), ScopusResults.class);

                        if (results.searchResults.totalResults == 0) {
                            break;
                        }

                        totalPublications += results.searchResults.entries.size();
                        results.searchResults.entries.forEach(citationCount -> {
                            allCitationCounts.add(citationCount.citationCount);

                            documentPublicationService.findDocumentByCommonIdentifier(
                                    citationCount.doi,
                                    null, citationCount.id, null)
                                .ifPresent(document -> {
                                    if (Objects.isNull(totalCitationsIndicator) ||
                                        citationCount.citationCount == 0) {
                                        return;
                                    }

                                    var newCitationCountIndicator =
                                        documentIndicatorRepository.findIndicatorForCodeAndSourceDocumentId(
                                            totalCitationsIndicator.getCode(),
                                            EntityIndicatorSource.SCOPUS,
                                            document.getId()).orElse(new DocumentIndicator());

                                    newCitationCountIndicator.setDocument(document);
                                    newCitationCountIndicator.setNumericValue(
                                        (double) citationCount.citationCount);
                                    newCitationCountIndicator.setIndicator(totalCitationsIndicator);
                                    newCitationCountIndicator.setSource(
                                        EntityIndicatorSource.SCOPUS);
                                    newCitationCountIndicator.setToDate(LocalDate.now());
                                    documentIndicators.add(newCitationCountIndicator);
                                });
                        });

                        cursor = (Objects.nonNull(results.searchResults.cursor) &&
                            results.searchResults.entries.size() == 100) ?
                            results.searchResults.cursor.next : null;
                    }

                    externalIndicatorWorker.persistPersonCitationIndicators(
                        person, new HashMap<>(
                            Map.of("TOTAL", allCitationCounts.stream().reduce(0, Integer::sum))),
                        totalPublications, allCitationCounts, totalCitationsIndicator,
                        yearlyCitationsIndicator, totalOutputIndicator, hIndexIndicator,
                        EntityIndicatorSource.SCOPUS);

                    documentIndicatorRepository.saveAll(documentIndicators);
                } catch (HttpClientErrorException e) {
                    if (e.getMessage().contains("AUTHENTICATION_ERROR")) {
                        scopusAuthenticationHelper.refreshAuthentication();
                        if (retryCount < MAX_RETRY_COUNT) {
                            retryCount++;
                            continue;
                        }
                    }

                    log.error("Exception occurred during document fetching: {}", e.getMessage());
                    shouldRetry = false;
                } catch (JsonProcessingException e) {
                    log.error("JSON parsing error in Scopus response: {}", e.getMessage());
                    shouldRetry = false;
                } catch (ResourceAccessException e) {
                    log.error("Exception occurred during connection to Scopus: {}", e.getMessage());
                    shouldRetry = false;
                }
            }
        }
    }

    private HashMap<String, Integer> accumulateCitationCounts(
        List<OpenAlexPublicationCitationCount> citationCounts,
        HashMap<String, Integer> existingMap
    ) {
        if (Objects.isNull(existingMap)) {
            existingMap = new HashMap<>();
        }

        for (OpenAlexPublicationCitationCount record : citationCounts) {
            if (Objects.nonNull(record.citationCount()) && record.citationCount() > 0) {
                existingMap.merge("TOTAL", record.citationCount(), Integer::sum);

                if (Objects.nonNull(record.citationsByYear())) {
                    for (YearlyCitations yearly : record.citationsByYear()) {
                        if (Objects.nonNull(yearly.citationCount()) &&
                            Objects.nonNull(yearly.year())) {
                            String yearKey = yearly.year().toString();
                            existingMap.merge(yearKey, yearly.citationCount(), Integer::sum);
                        }
                    }
                }
            }
        }

        return existingMap;
    }

    private void performDataEnrichment(Person person) {
        var baseURL = "https://api.openalex.org/authors";

        if (StringUtil.valueExists(person.getOpenAlexId())) {
            baseURL += "/" + person.getOpenAlexId();
        } else if (StringUtil.valueExists(person.getOrcid())) {
            baseURL += "?filter=orcid:" + person.getOrcid();
        } else if (StringUtil.valueExists(person.getScopusAuthorId())) {
            baseURL += "?filter=scopus:" + person.getScopusAuthorId();
        } else {
            return;
        }

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            ResponseEntity<String> responseEntity =
                restTemplateProvider.provideRestTemplate()
                    .getForEntity(baseURL, String.class);

            if (responseEntity.getStatusCode() != HttpStatus.OK) {
                return;
            }

            var response =
                objectMapper.readValue(responseEntity.getBody(), OpenAlexResponse.class);
            if (Objects.isNull(response) || Objects.isNull(response.results()) ||
                response.results().isEmpty()) {
                return;
            }

            var result = response.results.getFirst();
            if (!StringUtil.valueExists(person.getOpenAlexId()) &&
                result.identifiers().containsKey("openalex")) {
                var identifier =
                    result.identifiers().get("openalex").replace("https://openalex.org/", "");
                if (!personService.isIdentifierInUse(identifier, person.getId())) {
                    person.setOpenAlexId(identifier);
                }
            }

            if (!StringUtil.valueExists(person.getOrcid()) &&
                result.identifiers().containsKey("orcid")) {
                var identifier =
                    result.identifiers().get("orcid").replace("https://orcid.org/", "");
                if (!personService.isIdentifierInUse(identifier, person.getId())) {
                    person.setOrcid(identifier);
                }
            }

            if (!StringUtil.valueExists(person.getScopusAuthorId()) &&
                result.identifiers().containsKey("scopus")) {
                var identifier = result.identifiers().get("scopus").split("&")[0].replace(
                    "http://www.scopus.com/inward/authorDetails.url?authorID=", "");
                if (!personService.isIdentifierInUse(identifier, person.getId())) {
                    person.setScopusAuthorId(identifier);
                }
            }

            personService.save(person);
            personIndexRepository.findByDatabaseId(person.getId())
                .ifPresent(personIndex -> {
                    personIndex.setOpenAlexId(person.getOpenAlexId());
                    personIndex.setOrcid(person.getOrcid());
                    personIndex.setScopusAuthorId(person.getScopusAuthorId());

                    personIndexRepository.save(personIndex);
                });
        } catch (Exception e) {
            log.warn("Unable to fetch author data from OpenAlex for {}. Reason: {}",
                baseURL.replace("https://api.openalex.org/authors/", ""), e.getMessage());
        }
    }

    @Nullable
    private String constructAdequateOpenAlexSearchFilter(Person person) {
        if (StringUtil.valueExists(person.getOpenAlexId())) {
            return "author.id:" + person.getOpenAlexId();
        } else if (StringUtil.valueExists(person.getOrcid())) {
            return "author.orcid:" + person.getOrcid();
        }

        return null;
    }

    private void updateDocumentCitationCounts(OpenAlexResults results) {
        results.citationCounts.forEach(publicationCitations -> {
            if (Objects.isNull(publicationCitations.doi())) {
                return;
            }

            documentPublicationIndexRepository.findByDoi(
                    publicationCitations.doi().replace("https://doi.org/", ""))
                .ifPresent(docIndex -> {
                    docIndex.setTotalCitations(
                        (long) publicationCitations.citationCount);
                    documentPublicationIndexRepository.save(docIndex);
                });
        });
    }

    @EventListener
    @Transactional(readOnly = true)
    protected void handleExternalIndicatorReindexing(ReindexExternalIndicatorsEvent event) {
        if (Objects.isNull(event) || Objects.isNull(event.index())) {
            return;
        }

        if (event.index() instanceof PersonIndex) {
            reindexPersonIndicators((PersonIndex) event.index(),
                List.of(EntityIndicatorSource.OPEN_ALEX));
        } else if (event.index() instanceof DocumentPublicationIndex) {
            reindexDocumentIndicators((DocumentPublicationIndex) event.index(),
                List.of(EntityIndicatorSource.OPEN_ALEX));
        }
    }

    public void reindexPersonIndicators(PersonIndex index,
                                        List<EntityIndicatorSource> externalSources) {
        var personId = index.getDatabaseId();

        var indicators =
            personIndicatorRepository.findIndicatorsForPersonAndSources(personId, externalSources);

        index.setTotalCitations(0L);
        index.setHIndex(0);
        index.getCitationsByYear().clear();

        for (var indicator : indicators) {
            var code = indicator.getIndicator().getCode();
            var value = indicator.getNumericValue() != null ? indicator.getNumericValue() : 0.0;
            var fromDate = indicator.getFromDate();

            switch (code) {
                case "totalCitations":
                    index.setTotalCitations((long) value);
                    break;
                case "hIndex":
                    index.setHIndex((int) value);
                    break;
                case "yearlyCitations":
                    if (fromDate != null) {
                        index.getCitationsByYear().put(fromDate.getYear(), (int) value);
                    }
                    break;
                default:
                    break;
            }
        }

        personIndexRepository.save(index);
    }

    public void reindexDocumentIndicators(DocumentPublicationIndex index,
                                          List<EntityIndicatorSource> externalSources) {
        var documentId = index.getDatabaseId();

        var indicators = documentIndicatorRepository.findIndicatorsForDocumentAndSources(documentId,
            externalSources);

        index.setTotalCitations(0L);

        for (var indicator : indicators) {
            var code = indicator.getIndicator().getCode();
            var value = indicator.getNumericValue() != null ? indicator.getNumericValue() : 0.0;

            switch (code) {
                case "totalCitations":
                    index.setTotalCitations((long) value);
                    break;
                default:
                    break;
            }
        }

        documentPublicationIndexRepository.save(index);
    }

    private Map<MetricType, List<Integer>> harvestDocumentMetrics(
        Person person,
        List<DocumentMetricHarvester> harvesters
    ) {
        var metricValues =
            new EnumMap<MetricType, List<Integer>>(MetricType.class);

        var harvestPeriodOffset = harvestPeriodOffsets.get("openCitations");
        var endYear = LocalDate.now().getYear();
        var startYear = endYear - harvestPeriodOffset;

        FunctionalUtil.forEachChunked(
            PageRequest.of(
                0,
                PROCESS_BATCH_SIZE,
                Sort.by(Sort.Direction.ASC, "databaseId")
            ),
            pageable -> documentPublicationIndexRepository
                .findByAuthorIdAndYearRangeOrUnknown(person.getId(), startYear, endYear, pageable),
            personDocuments -> {
                for (var doc : personDocuments) {
                    var doi = doc.getDoi();

                    if (Objects.isNull(doi) || doi.isBlank()) {
                        continue;
                    }

                    for (var harvester : harvesters) {
                        harvester.harvest(doi)
                            .ifPresent(result -> {
                                if (result.metricType().equals(MetricType.OPEN_ACCESS)) {
                                    externalIndicatorWorker.setOpenAccessPolicyInfo(doi, result);
                                }

                                metricValues
                                    .computeIfAbsent(
                                        result.metricType(),
                                        k -> new ArrayList<>())
                                    .add(result.value());
                            });
                    }
                }
            });

        return metricValues;
    }

    @Async("taskExecutor")
    @EventListener
    protected void handleManualIndicatorHarvest(HarvestExternalIndicatorsEvent ignored) {
        performIndicatorHarvest();
    }

    @Scheduled(cron = "${harvest-external-indicators.schedule}")
    protected void performScheduledIndicatorHarvest() {
        performIndicatorHarvest();
    }

    private void performIndicatorHarvest() {
        if (!harvestAllowed) {
            return;
        }

        if (!harvestLock.tryLock()) {
            log.info("Harvest already in progress, skipping execution");
            return;
        }

        try {
            performPersonIndicatorHarvest();
            performOUIndicatorDeduction();
        } finally {
            harvestLock.unlock();
        }
    }

    public record OpenAlexResults(
        @JsonProperty("results") List<OpenAlexPublicationCitationCount> citationCounts,
        Meta meta
    ) {
    }

    public record Meta(
        @JsonProperty("next_cursor")
        String nextCursor
    ) {
    }

    public record OpenAlexPublicationCitationCount(
        String id,
        @JsonProperty("doi") String doi,
        @JsonProperty("cited_by_count") Integer citationCount,
        @JsonProperty("counts_by_year") List<YearlyCitations> citationsByYear
    ) {
    }

    public record YearlyCitations(
        @JsonProperty("cited_by_count") Integer citationCount,
        Integer year
    ) {
    }

    public record ScopusResults(
        @JsonProperty("search-results") SearchResults searchResults
    ) {
    }

    public record SearchResults(
        @JsonProperty("opensearch:totalResults") Integer totalResults,
        ScopusCursor cursor,
        @JsonProperty("entry") List<ScopusEntry> entries
    ) {
    }

    public record ScopusEntry(
        @JsonProperty("prism:doi") String doi,
        @JsonProperty("dc:identifier") String id,
        @JsonProperty("citedby-count") Integer citationCount
    ) {
    }

    public record ScopusCursor(
        @JsonProperty("@next") String next
    ) {
    }

    public record OpenAlexResponse(
        @JsonProperty("results") List<IdentifierResult> results
    ) {
    }

    public record IdentifierResult(
        @JsonProperty("ids") Map<String, String> identifiers
    ) {
    }

    private record HarvestContext(
        Indicator totalCitationsIndicator,
        Indicator yearlyCitationsIndicator,
        Indicator totalOutputIndicator,
        Indicator hIndexIndicator,
        AtomicInteger openAlexRateLimit,
        AtomicInteger scopusRateLimit,
        AtomicInteger openCitationsRateLimit,
        AtomicInteger unpaywallRateLimit
    ) {
    }

    private record DeductionContext(
        Indicator totalCitationsIndicator,
        Indicator totalOutputIndicator,
        List<EntityIndicatorSource> sources
    ) {
    }
}
