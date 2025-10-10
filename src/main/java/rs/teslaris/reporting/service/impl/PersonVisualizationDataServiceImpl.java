package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.repository.person.InvolvementRepository;
import rs.teslaris.core.repository.user.UserRepository;
import rs.teslaris.core.service.interfaces.person.PersonService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.PersonVisualizationDataService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonVisualizationDataServiceImpl implements PersonVisualizationDataService {

    private final ElasticsearchClient elasticsearchClient;

    private final PersonService personService;

    private final UserRepository userRepository;

    private final InvolvementRepository involvementRepository;

    private final PersonIndexRepository personIndexRepository;


    @Override
    public List<YearlyCounts> getPublicationCountsForPerson(Integer personId, Integer startYear,
                                                            Integer endYear) {
        var yearRange = constructYearRange(startYear, endYear, personId);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var yearlyCounts = new ArrayList<YearlyCounts>();
        for (int i = yearRange.a; i <= yearRange.b; i++) {
            try {
                yearlyCounts.add(
                    new YearlyCounts(i, getPublicationCountsByTypeForAuthorAndYear(personId, i)));
            } catch (IOException e) {
                log.warn("Unable to fetch person publication counts for {}. Adding all zeros.", i);
            }
        }

        return yearlyCounts;
    }

    @Override
    public List<MCategoryCounts> getPersonPublicationsByMCategories(Integer personId,
                                                                    Integer startYear,
                                                                    Integer endYear) {
        var result = new ArrayList<MCategoryCounts>();

        var commissions = new HashSet<Pair<Integer, Set<MultiLingualContent>>>();
        involvementRepository.findActiveEmploymentInstitutionIds(personId)
            .forEach(institutionId ->
                commissions.addAll(
                    userRepository.findUserCommissionForOrganisationUnit(institutionId).stream()
                        .map(c -> new Pair<>(c.getId(), c.getDescription())).toList()));

        commissions.forEach(commission -> {
            SearchResponse<Void> response;
            try {
                response = elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(0)
                        .query(q -> q
                            .bool(b -> b
                                .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                .must(m -> m.term(t -> t.field("author_ids").value(personId)))
                                .must(m -> m.range(
                                    t -> t.field("year").gte(JsonData.of(startYear))
                                        .lte(JsonData.of(endYear))))
                                .must(m -> m.bool(sb -> sb
                                    .should(sn -> sn.term(
                                        t -> t.field("commission_assessment_groups.a")
                                            .value(commission.a)))
                                    .should(sn -> sn.bool(nb -> nb.mustNot(
                                        mn -> mn.exists(
                                            e -> e.field("commission_assessment_groups.a")))))
                                    .minimumShouldMatch("1")
                                ))
                            )
                        )
                        .aggregations("by_m_category", a -> a
                            .terms(t -> t.field("commission_assessment_groups.b.keyword")
                                .missing("NONE")
                                .size(11)) // 10 M categories + non-classified
                        ),
                    Void.class
                );
            } catch (IOException e) {
                log.warn("Unable to fetch person M categories for IDs (P:{}; C:{}).", personId,
                    commission.a);
                return;
            }

            var countsByCategory = new HashMap<String, Long>();
            response
                .aggregations()
                .get("by_m_category")
                .sterms()
                .buckets()
                .array()
                .forEach(bucket -> {
                    String mCategory = bucket.key().stringValue();
                    long count = bucket.docCount();
                    countsByCategory.put(mCategory,
                        countsByCategory.getOrDefault(mCategory, 0L) + count);
                });

            result.add(new MCategoryCounts(
                MultilingualContentConverter.getMultilingualContentDTO(commission.b),
                countsByCategory));
        });

        return result;
    }

    @Override
    public List<CommissionYearlyCounts> getMCategoryCountsForPerson(Integer personId,
                                                                    Integer startYear,
                                                                    Integer endYear) {
        var yearRange = constructYearRange(startYear, endYear, personId);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var commissions = new HashSet<Pair<Integer, Set<MultiLingualContent>>>();
        involvementRepository.findActiveEmploymentInstitutionIds(personId)
            .forEach(institutionId ->
                commissions.addAll(
                    userRepository.findUserCommissionForOrganisationUnit(institutionId).stream()
                        .map(c -> new Pair<>(c.getId(), c.getDescription())).toList()));

        var commissionYearlyCounts = new ArrayList<CommissionYearlyCounts>();
        for (var commission : commissions) {
            var yearlyCounts = new ArrayList<YearlyCounts>();

            for (int i = yearRange.a; i <= yearRange.b; i++) {
                try {
                    yearlyCounts.add(new YearlyCounts(i,
                        getPublicationMCategoryCountsByTypeForAuthorAndCommissionAndYear(personId,
                            commission.a, i)));

                } catch (IOException e) {
                    log.warn("Unable to fetch person M category counts for {}. Adding all zeros.",
                        i);
                }
            }

            commissionYearlyCounts.add(new CommissionYearlyCounts(
                MultilingualContentConverter.getMultilingualContentDTO(commission.b),
                yearlyCounts));
        }

        return commissionYearlyCounts;
    }

    @Override
    public List<StatisticsByCountry> getByCountryStatisticsForPerson(Integer personId,
                                                                     LocalDate from, LocalDate to) {
        var allMergedPersonIds = getAllMergedPersonIds(personId);

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("person_id").terms(
                                v -> v.value(allMergedPersonIds.stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )))
                            .must(m -> m.term(t -> t.field("is_bot").value(false)))
                            .must(m -> m.term(t -> t.field("type").value("VIEW")))
                            .must(m -> m.range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(from))
                                .lte(JsonData.of(to))
                            ))
                        )
                    )
                    .aggregations("by_country", a -> a
                        .terms(t -> t.field("country_code")
                            .size(
                                195)) // 195 countries exist at the moment, we can lower this if need be
                        .aggregations("country_name", sub -> sub
                            .terms(t -> t.field("country_name").size(1))
                        )
                    ),
                Void.class
            );
        } catch (IOException e) {
            log.warn("Unable to fetch person statistics for ID {}.", personId);
            return Collections.emptyList();
        }

        List<StatisticsByCountry> result = new ArrayList<>();

        response.aggregations()
            .get("by_country").sterms().buckets().array()
            .forEach(bucket -> {
                String countryCode = bucket.key().stringValue();
                long views = bucket.docCount();

                String countryName = bucket.aggregations()
                    .get("country_name").sterms().buckets().array()
                    .stream()
                    .findFirst()
                    .map(StringTermsBucket::key)
                    .map(FieldValue::stringValue)
                    .orElse(countryCode); // fallback, should never happen

                result.add(new StatisticsByCountry(countryCode, countryName, views));
            });

        return result;
    }

    @Override
    public Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer personId, LocalDate from,
                                                           LocalDate to) {
        var allMergedPersonIds = getAllMergedPersonIds(personId);

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("person_id").terms(
                                v -> v.value(allMergedPersonIds.stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )))
                            .must(m -> m.term(t -> t.field("is_bot").value(false)))
                            .must(m -> m.term(t -> t.field("type").value("VIEW")))
                            .must(m -> m.range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(from))
                                .lte(JsonData.of(to))
                            ))
                        )
                    )
                    .aggregations("per_month", a -> a
                        .dateHistogram(h -> h
                            .field("timestamp")
                            .calendarInterval(CalendarInterval.Month)
                            .minDocCount(0)
                            .extendedBounds(b -> b
                                .min(FieldDateMath.of(
                                    fdm -> fdm.expr(from.toString().substring(0, 7)))) // e.g. "2025-01"
                                .max(FieldDateMath.of(fdm -> fdm.expr(to.toString().substring(0, 7))))
                            )
                            .format("yyyy-MM")
                        )
                    ),
                Void.class
            );

            Map<YearMonth, Long> results = new LinkedHashMap<>();
            response.aggregations()
                .get("per_month")
                .dateHistogram()
                .buckets()
                .array()
                .forEach(bucket -> {
                    String keyAsString = bucket.keyAsString();
                    if (Objects.isNull(keyAsString)) {
                        return;
                    }

                    var ym = YearMonth.parse(keyAsString);
                    results.put(ym, bucket.docCount());
                });

            return results;
        } catch (IOException e) {
            log.error("Error fetching monthly statistics for person {} and type VIEW", personId);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<Year, Long> getYearlyStatisticsCounts(Integer personId, Integer startYear,
                                                     Integer endYear) {
        var allMergedPersonIds = getAllMergedPersonIds(personId);

        try {
            LocalDate from = LocalDate.of(startYear, 1, 1);
            LocalDate to = LocalDate.of(endYear, 12, 31);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("person_id").terms(
                                v -> v.value(allMergedPersonIds.stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )))
                            .must(m -> m.term(t -> t.field("is_bot").value(false)))
                            .must(m -> m.term(t -> t.field("type").value("VIEW")))
                            .must(m -> m.range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(from))
                                .lte(JsonData.of(to))
                            ))
                        )
                    )
                    .aggregations("per_year", a -> a
                        .dateHistogram(h -> h
                            .field("timestamp")
                            .calendarInterval(CalendarInterval.Year)
                            .minDocCount(0)
                            .extendedBounds(b -> b
                                .min(FieldDateMath.of(fdm -> fdm.expr(startYear.toString())))
                                .max(FieldDateMath.of(fdm -> fdm.expr(endYear.toString())))
                            )
                            .format("yyyy")
                        )
                    ),
                Void.class
            );

            Map<Year, Long> results = new LinkedHashMap<>();
            response.aggregations()
                .get("per_year")
                .dateHistogram()
                .buckets()
                .array()
                .forEach(bucket -> {
                    String keyAsString = bucket.keyAsString();
                    if (Objects.isNull(keyAsString)) {
                        return;
                    }

                    var y = Year.parse(keyAsString);
                    results.put(y, bucket.docCount());
                });

            return results;
        } catch (IOException e) {
            log.error("Error fetching yearly statistics for person {} and type VIEW", personId);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<Year, Long> getYearlyCitationCounts(Integer personId, Integer startYear,
                                                   Integer endYear) {
        var result = new TreeMap<Year, Long>();
        personIndexRepository.findByDatabaseId(personId).ifPresent(personIndex -> {
            personIndex.getCitationsByYear().forEach((key, value) -> {
                if (key >= startYear && key <= endYear) {
                    result.put(Year.of(key), Long.valueOf(value));
                }
            });
        });

        return result;
    }

    private Map<String, Long> getPublicationCountsByTypeForAuthorAndYear(Integer authorId,
                                                                         Integer year)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0) // no hits, only aggregations
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("is_approved").value(true)))
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                        .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                    )
                )
                .aggregations("by_type", a -> a
                    .terms(t -> t.field("type").size(9)) // for 9 document types
                ),
            Void.class
        );

        return response.aggregations()
            .get("by_type")
            .sterms()
            .buckets()
            .array()
            .stream()
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                MultiBucketBase::docCount
            ));
    }

    private Map<String, Long> getPublicationMCategoryCountsByTypeForAuthorAndCommissionAndYear(
        Integer authorId, Integer commissionId, Integer year)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("is_approved").value(true)))
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                        .must(m -> m.bool(sb -> sb
                            .should(sn -> sn.term(
                                t -> t.field("commission_assessment_groups.a").value(commissionId)))
                            .should(sn -> sn.bool(nb -> nb.mustNot(
                                mn -> mn.exists(e -> e.field("commission_assessment_groups.a")))))
                            .minimumShouldMatch("1")
                        ))
                    )
                )
                .aggregations("by_m_category", a -> a
                    .terms(t -> t.field("commission_assessment_groups.b.keyword")
                        .missing("NONE")
                        .size(11))
                ),
            Void.class
        );

        return response.aggregations()
            .get("by_m_category")
            .sterms()
            .buckets()
            .array()
            .stream()
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                MultiBucketBase::docCount
            ));
    }

    private Pair<Integer, Integer> findPublicationYearRange(Integer authorId) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(m -> m.term(t -> t.field("author_ids").value(authorId)))
                            .must(m -> m.range(t -> t.field("year").gt(JsonData.of(0))))
                        )
                    )
                    .aggregations("earliestYear", a -> a.min(m -> m.field("year")))
                    .aggregations("latestYear", a -> a.max(m -> m.field("year"))),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error finding publication year range for Person with ID {}.", authorId);
            return new Pair<>(null, null);
        }

        double min = response.aggregations().get("earliestYear").min().value();
        double max = response.aggregations().get("latestYear").max().value();

        return new Pair<>((int) min, (int) max);
    }

    private List<Integer> getAllMergedPersonIds(Integer personId) {
        var person = personService.findOne(personId);
        var allMergedPersonIds = new ArrayList<>(person.getMergedIds());
        allMergedPersonIds.add(personId);

        return allMergedPersonIds;
    }

    private Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear,
                                                      Integer personId) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var yearRange = findPublicationYearRange(personId);
            return new Pair<>(yearRange.a, yearRange.b);
        }

        return new Pair<>(startYear, endYear);
    }
}
