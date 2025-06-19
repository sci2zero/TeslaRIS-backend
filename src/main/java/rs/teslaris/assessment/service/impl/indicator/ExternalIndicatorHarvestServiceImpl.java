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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
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
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.person.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.FunctionalUtil;
import rs.teslaris.core.util.RestTemplateProvider;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExternalIndicatorHarvestServiceImpl implements ExternalIndicatorHarvestService {

    private final RestTemplateProvider restTemplateProvider;

    private final DocumentPublicationService documentPublicationService;

    private final PersonService personService;

    private final PersonIndicatorRepository personIndicatorRepository;

    private final DocumentIndicatorRepository documentIndicatorRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final OrganisationUnitIndicatorRepository organisationUnitIndicatorRepository;

    private final OrganisationUnitService organisationUnitService;

    private final IndicatorService indicatorService;


    @Override
    public void performOUIndicatorDeduction() {
        var totalCitationsIndicator = indicatorService.getIndicatorByCode("totalCitations");
        var totalOutputIndicator = indicatorService.getIndicatorByCode("totalOutputCount");

        var entityIndicatorSources =
            List.of(EntityIndicatorSource.OPEN_ALEX); // TODO: Add Scopus and OpenCitations

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
                            personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                                totalCitationsIndicator.getCode(), entityIndicatorSource,
                                null, person.getDatabaseId()
                            ).ifPresent(ind -> totalCitationCount.addAndGet(ind.getNumericValue()));

                            personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                                totalOutputIndicator.getCode(), entityIndicatorSource, null,
                                person.getDatabaseId()
                            ).ifPresent(
                                ind -> totalPublicationsCount.addAndGet(ind.getNumericValue()));
                        })
                    );

                    var organisationUnit =
                        organisationUnitService.findOne(institution.getDatabaseId());

                    organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                        totalCitationsIndicator.getCode(), entityIndicatorSource,
                        institution.getDatabaseId()
                    ).ifPresent(organisationUnitIndicatorRepository::delete);

                    var newTotalCitationsIndicator = new OrganisationUnitIndicator();
                    newTotalCitationsIndicator.setOrganisationUnit(organisationUnit);
                    newTotalCitationsIndicator.setNumericValue(totalCitationCount.doubleValue());
                    newTotalCitationsIndicator.setSource(entityIndicatorSource);
                    newTotalCitationsIndicator.setIndicator(totalCitationsIndicator);
                    newTotalCitationsIndicator.setToDate(LocalDate.now());

                    organisationUnitIndicatorRepository.findIndicatorForCodeAndSourceAndOrganisationUnitId(
                        totalOutputIndicator.getCode(), entityIndicatorSource,
                        institution.getDatabaseId()
                    ).ifPresent(organisationUnitIndicatorRepository::delete);

                    var newTotalOutputIndicator = new OrganisationUnitIndicator();
                    newTotalOutputIndicator.setOrganisationUnit(organisationUnit);
                    newTotalOutputIndicator.setNumericValue(totalPublicationsCount.doubleValue());
                    newTotalOutputIndicator.setSource(entityIndicatorSource);
                    newTotalOutputIndicator.setIndicator(totalOutputIndicator);
                    newTotalOutputIndicator.setToDate(LocalDate.now());

                    organisationUnitIndicatorRepository.saveAll(
                        List.of(newTotalCitationsIndicator, newTotalOutputIndicator));
                });
            })
        );
    }

    @Override
    public void performPersonIndicatorHarvest() {
        var totalCitationsIndicator = indicatorService.getIndicatorByCode("totalCitations");
        var yearlyCitationsIndicator = indicatorService.getIndicatorByCode("yearlyCitations");
        var totalOutputIndicator = indicatorService.getIndicatorByCode("totalOutputCount");
        var hIndexIndicator = indicatorService.getIndicatorByCode("hIndex");

        // TODO: Move in configuration file
        AtomicInteger openAlexRateLimit = new AtomicInteger(2000);

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
            })
        );
    }

    private void harvestFromOpenAlex(Person person, Indicator totalCitationsIndicator,
                                     Indicator yearlyCitationsIndicator,
                                     Indicator totalOutputIndicator, Indicator hIndexIndicator) {
        var baseUrl = "https://api.openalex.org/works?per-page=100" +
            "&filter=author.id:" + person.getOpenAlexId() +
            ",from_publication_date:2000-01-01,to_publication_date:" + LocalDate.now();

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
                                citationCount.id)
                            .ifPresent(document -> {
                                documentIndicatorRepository.findIndicatorForCodeAndDocumentId(
                                        totalCitationsIndicator.getCode(), document.getId())
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
                allCitationCounts,
                totalCitationsIndicator, yearlyCitationsIndicator, totalOutputIndicator,
                hIndexIndicator);

        } catch (HttpClientErrorException e) {
            log.error("HTTP error fetching OpenAlex works: {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
        }
    }

    private void persistPersonCitationIndicators(Person person, Map<String, Integer> counts,
                                                 Integer totalOutputCount,
                                                 List<Integer> citations,
                                                 Indicator totalIndicator,
                                                 Indicator yearlyIndicator,
                                                 Indicator totalOutputIndicator,
                                                 Indicator hIndexIndicator) {
        counts.forEach((key, value) -> {
            if (value == 0) {
                return;
            }

            var newCitationCountIndicator = new PersonIndicator();
            newCitationCountIndicator.setPerson(person);
            newCitationCountIndicator.setNumericValue((double) value);
            newCitationCountIndicator.setSource(EntityIndicatorSource.OPEN_ALEX);

            if (key.equals("TOTAL")) {
                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        totalIndicator.getCode(), EntityIndicatorSource.OPEN_ALEX, null, person.getId())
                    .ifPresent(personIndicatorRepository::delete);
                newCitationCountIndicator.setIndicator(totalIndicator);
                newCitationCountIndicator.setToDate(LocalDate.now());
            } else {
                int year = Integer.parseInt(key);
                var fromDate = LocalDate.of(year, 1, 1);

                personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                        yearlyIndicator.getCode(), EntityIndicatorSource.OPEN_ALEX, year,
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
        personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                hIndexIndicator.getCode(), EntityIndicatorSource.OPEN_ALEX, null, person.getId())
            .ifPresent(personIndicatorRepository::delete);
        var newHIndexIndicator = new PersonIndicator();
        newHIndexIndicator.setPerson(person);
        newHIndexIndicator.setNumericValue((double) hIndex);
        newHIndexIndicator.setSource(EntityIndicatorSource.OPEN_ALEX);
        newHIndexIndicator.setIndicator(hIndexIndicator);
        newHIndexIndicator.setToDate(LocalDate.now());

        personIndicatorRepository.findIndicatorForCodeAndSourceAndFromDateAndPersonId(
                totalOutputIndicator.getCode(), EntityIndicatorSource.OPEN_ALEX, null, person.getId())
            .ifPresent(personIndicatorRepository::delete);
        var newTotalOutputIndicator = new PersonIndicator();
        newTotalOutputIndicator.setPerson(person);
        newTotalOutputIndicator.setNumericValue((double) totalOutputCount);
        newTotalOutputIndicator.setSource(EntityIndicatorSource.OPEN_ALEX);
        newTotalOutputIndicator.setIndicator(totalOutputIndicator);
        newTotalOutputIndicator.setToDate(LocalDate.now());

        personIndicatorRepository.saveAll(List.of(newHIndexIndicator, newTotalOutputIndicator));
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
}
