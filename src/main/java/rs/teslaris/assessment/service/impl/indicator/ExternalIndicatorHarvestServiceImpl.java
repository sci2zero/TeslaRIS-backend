package rs.teslaris.assessment.service.impl.indicator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AtomicDouble;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import rs.teslaris.assessment.model.indicator.DocumentIndicator;
import rs.teslaris.assessment.model.indicator.EntityIndicatorSource;
import rs.teslaris.assessment.model.indicator.Indicator;
import rs.teslaris.assessment.model.indicator.OrganisationUnitIndicator;
import rs.teslaris.assessment.model.indicator.PersonIndicator;
import rs.teslaris.assessment.repository.indicator.DocumentIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.OrganisationUnitIndicatorRepository;
import rs.teslaris.assessment.repository.indicator.PersonIndicatorRepository;
import rs.teslaris.assessment.service.interfaces.indicator.ExternalIndicatorHarvestService;
import rs.teslaris.assessment.service.interfaces.indicator.IndicatorService;
import rs.teslaris.assessment.util.ExternalMappingConstraintType;
import rs.teslaris.assessment.util.IndicatorMappingConfigurationLoader;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.FunctionalUtil;
import rs.teslaris.core.util.RestTemplateProvider;
import rs.teslaris.core.util.ScopusAuthenticationHelper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExternalIndicatorHarvestServiceImpl implements ExternalIndicatorHarvestService {

    private final RestTemplateProvider restTemplateProvider;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonService personService;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final OrganisationUnitService organisationUnitService;

    private final IndicatorService indicatorService;

    private final ScopusAuthenticationHelper scopusAuthenticationHelper;

    private Map<String, String> externalIndicatorMapping;

    private Map<String, Integer> harvestPeriodOffsets;

    private Map<String, Integer> rateLimits;


    @Override
    public void performOUIndicatorDeduction() {
        refreshConfiguration();

        var totalCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalCitationCount", null));
        var totalOutputIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalPublicationCount", null));

        var entityIndicatorSources =
            List.of(
                EntityIndicatorSource.OPEN_ALEX,
                EntityIndicatorSource.OPEN_CITATIONS,
                EntityIndicatorSource.SCOPUS
            );

        FunctionalUtil.forEachChunked(
            PageRequest.of(0, 50),
            organisationUnitIndexRepository::findAll,
            institutions -> institutions.forEach(institution -> {
                entityIndicatorSources.forEach(entityIndicatorSource -> {
                    var totalCitationCount = new AtomicDouble(0);
                    var totalPublicationsCount = new AtomicDouble(0);

                    FunctionalUtil.forEachChunked(
                        PageRequest.of(0, 50),
                        page -> personService.findPeopleForOrganisationUnit(
                            institution.getDatabaseId(),
                            page, false),
                        people -> people.forEach(person -> {
                            if (Objects.nonNull(totalCitationsIndicator)) {
                                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                                    totalCitationsIndicator.getCode(), entityIndicatorSource,
                                    null, person.getDatabaseId()
                                ).ifPresent(
                                    ind -> totalCitationCount.addAndGet(ind.getNumericValue()));
                            }

                            if (Objects.nonNull(totalOutputIndicator)) {
                                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                                    totalOutputIndicator.getCode(), entityIndicatorSource, null,
                                    person.getDatabaseId()
                                ).ifPresent(
                                    ind -> totalPublicationsCount.addAndGet(ind.getNumericValue()));
                            }
                        })
                    );

                    var organisationUnit =
                        organisationUnitService.findOne(institution.getDatabaseId());

                    if (Objects.nonNull(totalCitationsIndicator) && totalCitationCount.get() > 0) {
                        organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                            totalCitationsIndicator.getCode(), entityIndicatorSource,
                            institution.getDatabaseId()
                        ).ifPresent(organisationUnitIndicatorRepository::delete);

                        var newTotalCitationsIndicator = new OrganisationUnitIndicator();
                        newTotalCitationsIndicator.setOrganisationUnit(organisationUnit);
                        newTotalCitationsIndicator.setNumericValue(
                            totalCitationCount.doubleValue());
                        newTotalCitationsIndicator.setSource(entityIndicatorSource);
                        newTotalCitationsIndicator.setIndicator(totalCitationsIndicator);
                        newTotalCitationsIndicator.setToDate(LocalDate.now());

                        organisationUnitIndicatorRepository.save(newTotalCitationsIndicator);
                    }

                    if (Objects.nonNull(totalOutputIndicator) && totalPublicationsCount.get() > 0) {
                        organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                            totalOutputIndicator.getCode(), entityIndicatorSource,
                            institution.getDatabaseId()
                        ).ifPresent(organisationUnitIndicatorRepository::delete);

                        var newTotalOutputIndicator = new OrganisationUnitIndicator();
                        newTotalOutputIndicator.setOrganisationUnit(organisationUnit);
                        newTotalOutputIndicator.setNumericValue(
                            totalPublicationsCount.doubleValue());
                        newTotalOutputIndicator.setSource(entityIndicatorSource);
                        newTotalOutputIndicator.setIndicator(totalOutputIndicator);
                        newTotalOutputIndicator.setToDate(LocalDate.now());

                        organisationUnitIndicatorRepository.save(newTotalOutputIndicator);
                    }
                });
            })
        );
    }

    @Override
    public void performPersonIndicatorHarvest() {
        refreshConfiguration();

        var totalCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalCitationCount", null));
        var yearlyCitationsIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("yearlyCitations", null));
        var totalOutputIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("totalPublicationCount", null));
        var hIndexIndicator = indicatorService.getIndicatorByCode(
            externalIndicatorMapping.getOrDefault("hIndex", null));

        var openAlexRateLimit = new AtomicInteger(rateLimits.getOrDefault("openAlex", 0));
        var scopusRateLimit = new AtomicInteger(rateLimits.getOrDefault("scopus", 0));
        var openCitationsRateLimit = new AtomicInteger(rateLimits.getOrDefault("openCitations", 0));

        FunctionalUtil.forEachChunked(
            PageRequest.of(0, 50),
            personService::findPersonsByLRUHarvest,
            people -> people.forEach(person -> {
                if (Objects.nonNull(person.getOpenAlexId()) && !person.getOpenAlexId().isBlank() &&
                    openAlexRateLimit.getAndDecrement() > 0) {
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
                if (openCitationsRateLimit.getAndDecrement() > 0) {
                    harvestFromOpenCitations(
                        person,
                        totalCitationsIndicator,
                        yearlyCitationsIndicator,
                        totalOutputIndicator,
                        hIndexIndicator
                    );
                }
            })
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

        var baseUrl = "https://api.openalex.org/works?per-page=100" +
            "&filter=author.id:" + person.getOpenAlexId() +
            ",from_publication_date:" + startDate + ",to_publication_date:" + endDate;

        var cursor = "*";
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            HashMap<String, Integer> personAggregatedCounts = new HashMap<>();
            List<Integer> allCitationCounts = new ArrayList<>();
            int totalPublications = 0;

            while (Objects.nonNull(cursor)) {
                String paginatedUrl = baseUrl + "&cursor=" + cursor;
                ResponseEntity<String> responseEntity =
                    restTemplateProvider.provideRestTemplate()
                        .getForEntity(paginatedUrl, String.class);

                if (responseEntity.getStatusCode() != HttpStatus.OK) {
                    break;
                }

                var results =
                    objectMapper.readValue(responseEntity.getBody(), OpenAlexResults.class);

                if (Objects.nonNull(results.citationCounts()) &&
                    !results.citationCounts.isEmpty()) {
                    var citationCounts = results.citationCounts.stream()
                        .filter(citationResult -> citationResult.citationCount > 0).toList();
                    totalPublications += results.citationCounts.size();

                    personAggregatedCounts =
                        accumulateCitationCounts(citationCounts, personAggregatedCounts);

                    citationCounts.forEach(citationCount -> {
                        allCitationCounts.add(citationCount.citationCount);

                        documentPublicationService.findDocumentByCommonIdentifier(citationCount.doi,
                                citationCount.id, null)
                            .ifPresent(document -> {
                                if (Objects.isNull(totalCitationsIndicator)) {
                                    return;
                                }

                                documentIndicatorRepository.findIndicatorForCodeAndSourceDocumentId(
                                        totalCitationsIndicator.getCode(),
                                        EntityIndicatorSource.OPEN_ALEX, document.getId())
                                    .ifPresent(documentIndicatorRepository::delete);

                                var newCitationCountIndicator = new DocumentIndicator();
                                newCitationCountIndicator.setDocument(document);
                                newCitationCountIndicator.setNumericValue(
                                    (double) citationCount.citationCount);
                                newCitationCountIndicator.setIndicator(totalCitationsIndicator);
                                newCitationCountIndicator.setSource(
                                    EntityIndicatorSource.OPEN_ALEX);
                                newCitationCountIndicator.setToDate(LocalDate.now());
                                documentIndicatorRepository.save(newCitationCountIndicator);
                            });
                    });
                }

                cursor = Objects.nonNull(results.meta()) ? results.meta().nextCursor() : null;
            }

            persistPersonCitationIndicators(person, personAggregatedCounts, totalPublications,
                allCitationCounts, totalCitationsIndicator, yearlyCitationsIndicator,
                totalOutputIndicator, hIndexIndicator, EntityIndicatorSource.OPEN_ALEX);

        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching OpenAlex works: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Exception occurred during connection to OpenAlex: {}", e.getMessage());
        }
    }

    private void harvestFromOpenCitations(Person person,
                                          Indicator totalCitationsIndicator,
                                          Indicator yearlyCitationsIndicator,
                                          Indicator totalOutputIndicator,
                                          Indicator hIndexIndicator) {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            List<Integer> allCitationCounts = new ArrayList<>();
            var totalPublications = new AtomicInteger(0);

            var harvestPeriodOffset = harvestPeriodOffsets.get("openCitations");
            var endYear = LocalDate.now().getYear();
            var startYear = endYear - harvestPeriodOffset;
            FunctionalUtil.forEachChunked(PageRequest.of(0, 50),
                (pageable) -> documentPublicationIndexRepository.findByAuthorIdAndYearRangeOrUnknown(
                    person.getId(), startYear, endYear, pageable), (personDocuments) -> {
                    for (var doc : personDocuments) {
                        var doi = doc.getDoi();
                        if (Objects.isNull(doi) || doi.isBlank()) {
                            continue;
                        }

                        var url =
                            "https://opencitations.net/index/api/v2/citation-count/doi:" + doi;
                        ResponseEntity<String> responseEntity =
                            restTemplateProvider.provideRestTemplate()
                                .getForEntity(url, String.class);

                        if (responseEntity.getStatusCode() != HttpStatus.OK) {
                            continue;
                        }

                        OpenCitationsEntry[] result;
                        try {
                            result = objectMapper.readValue(responseEntity.getBody(),
                                OpenCitationsEntry[].class);
                        } catch (JsonProcessingException e) {
                            log.error("JSON parsing error in OpenCitations response: {}",
                                e.getMessage());
                            continue;
                        }

                        if (result.length == 0) {
                            continue;
                        }

                        int citationCount = result[0].count();
                        totalPublications.getAndIncrement();
                        allCitationCounts.add(citationCount);
                    }
                });

            persistPersonCitationIndicators(person, new HashMap<>(
                    Map.of("TOTAL", allCitationCounts.stream().reduce(0, Integer::sum))),
                totalPublications.get(), allCitationCounts, totalCitationsIndicator,
                yearlyCitationsIndicator, totalOutputIndicator, hIndexIndicator,
                EntityIndicatorSource.OPEN_CITATIONS);

        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching OpenCitations data: {}", e.getMessage());
        } catch (ResourceAccessException e) {
            log.error("Exception occurred during connection to OpenCitations: {}", e.getMessage());
        }
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

            ResponseEntity<String> responseEntity;
            try {
                var cursor = "*";
                while (Objects.nonNull(cursor)) {
                    var url =
                        "https://api.elsevier.com/content/search/scopus?query=AU-ID(" +
                            person.getScopusAuthorId() +
                            ")&date=" + startYear + "-" + endYear +
                            "&count=100&view=STANDARD&cursor=" + cursor;

                    responseEntity =
                        restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(requestHeaders),
                            String.class);
                    var results =
                        objectMapper.readValue(responseEntity.getBody(), ScopusResults.class);

                    if (results.searchResults.totalResults == 0) {
                        break;
                    }

                    totalPublications += results.searchResults.entries.size();
                    results.searchResults.entries.forEach(citationCount -> {
                        allCitationCounts.add(citationCount.citationCount);

                        documentPublicationService.findDocumentByCommonIdentifier(citationCount.doi,
                                null,
                                citationCount.id)
                            .ifPresent(document -> {
                                if (Objects.isNull(totalCitationsIndicator) ||
                                    citationCount.citationCount == 0) {
                                    return;
                                }

                                documentIndicatorRepository.findIndicatorForCodeAndSourceDocumentId(
                                        totalCitationsIndicator.getCode(), EntityIndicatorSource.SCOPUS,
                                        document.getId())
                                    .ifPresent(documentIndicatorRepository::delete);

                                var newCitationCountIndicator = new DocumentIndicator();
                                newCitationCountIndicator.setDocument(document);
                                newCitationCountIndicator.setNumericValue(
                                    (double) citationCount.citationCount);
                                newCitationCountIndicator.setIndicator(totalCitationsIndicator);
                                newCitationCountIndicator.setSource(
                                    EntityIndicatorSource.SCOPUS);
                                newCitationCountIndicator.setToDate(LocalDate.now());
                                documentIndicatorRepository.save(newCitationCountIndicator);
                            });
                    });

                    cursor = (Objects.nonNull(results.searchResults.cursor) &&
                        results.searchResults.entries.size() == 100) ?
                        results.searchResults.cursor.next : null;
                }

                persistPersonCitationIndicators(person, new HashMap<>(
                        Map.of("TOTAL", allCitationCounts.stream().reduce(0, Integer::sum))),
                    totalPublications, allCitationCounts, totalCitationsIndicator,
                    yearlyCitationsIndicator, totalOutputIndicator, hIndexIndicator,
                    EntityIndicatorSource.SCOPUS);

            } catch (HttpClientErrorException e) {
                log.error("Exception occurred during document fetching: {}", e.getMessage());
            } catch (JsonProcessingException e) {
                log.error("JSON parsing error in Scopus response: {}", e.getMessage());
            } catch (ResourceAccessException e) {
                log.error("Exception occurred during connection to Scopus: {}", e.getMessage());
            }
        }
    }

    private void persistPersonCitationIndicators(Person person, Map<String, Integer> counts,
                                                 Integer totalOutputCount,
                                                 List<Integer> citations,
                                                 Indicator totalIndicator,
                                                 Indicator yearlyIndicator,
                                                 Indicator totalOutputIndicator,
                                                 Indicator hIndexIndicator,
                                                 EntityIndicatorSource source) {
        counts.forEach((key, value) -> {
            if (value == 0) {
                return;
            }

            var newCitationCountIndicator = new PersonIndicator();
            newCitationCountIndicator.setPerson(person);
            newCitationCountIndicator.setNumericValue((double) value);
            newCitationCountIndicator.setSource(source);

            if (key.equals("TOTAL")) {
                if (Objects.isNull(totalIndicator)) {
                    return;
                }

                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        totalIndicator.getCode(), source, null, person.getId())
                    .ifPresent(personIndicatorRepository::delete);
                newCitationCountIndicator.setIndicator(totalIndicator);
                newCitationCountIndicator.setToDate(LocalDate.now());
            } else {
                if (Objects.isNull(yearlyIndicator)) {
                    return;
                }

                int year = Integer.parseInt(key);
                var fromDate = LocalDate.of(year, 1, 1);

                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        yearlyIndicator.getCode(), source, year,
                        person.getId())
                    .ifPresent(personIndicatorRepository::delete);

                newCitationCountIndicator.setIndicator(yearlyIndicator);
                newCitationCountIndicator.setFromDate(fromDate);
                newCitationCountIndicator.setToDate(
                    year == LocalDate.now().getYear() ? LocalDate.now() : LocalDate.of(year, 12, 31)
                );
            }

            personIndicatorRepository.save(newCitationCountIndicator);
        });

        var hIndex = calculateHIndex(citations);
        if (Objects.nonNull(hIndexIndicator) && hIndex > 0) {
            personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                    hIndexIndicator.getCode(), source, null, person.getId())
                .ifPresent(personIndicatorRepository::delete);
            var newHIndexIndicator = new PersonIndicator();
            newHIndexIndicator.setPerson(person);
            newHIndexIndicator.setNumericValue((double) hIndex);
            newHIndexIndicator.setSource(source);
            newHIndexIndicator.setIndicator(hIndexIndicator);
            newHIndexIndicator.setToDate(LocalDate.now());
            personIndicatorRepository.save(newHIndexIndicator);
        }

        if (Objects.nonNull(totalOutputIndicator)) {
            personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                    totalOutputIndicator.getCode(), source, null, person.getId())
                .ifPresent(personIndicatorRepository::delete);
            var newTotalOutputIndicator = new PersonIndicator();
            newTotalOutputIndicator.setPerson(person);
            newTotalOutputIndicator.setNumericValue((double) totalOutputCount);
            newTotalOutputIndicator.setSource(source);
            newTotalOutputIndicator.setIndicator(totalOutputIndicator);
            newTotalOutputIndicator.setToDate(LocalDate.now());
            personIndicatorRepository.save(newTotalOutputIndicator);
        }
    }

    private int calculateHIndex(List<Integer> citations) {
        return (int) citations.stream()
            .sorted(Comparator.reverseOrder())
            .mapToInt(Integer::intValue)
            .takeWhile(new IntPredicate() {
                int index = 0;

                @Override
                public boolean test(int value) {
                    index++;
                    return value >= index;
                }
            })
            .count();
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

    @Scheduled(cron = "${harvest-external-indicators.schedule}")
    protected void performIndicatorHarvest() {
        performPersonIndicatorHarvest();
        performOUIndicatorDeduction();
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

    public record OpenCitationsEntry(
        Integer count
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
}
