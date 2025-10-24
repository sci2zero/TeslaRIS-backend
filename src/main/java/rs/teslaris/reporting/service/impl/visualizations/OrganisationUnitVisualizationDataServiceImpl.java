package rs.teslaris.reporting.service.impl.visualizations;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.visualizations.OrganisationUnitVisualizationDataService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationUnitVisualizationDataServiceImpl implements
    OrganisationUnitVisualizationDataService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitService organisationUnitService;


    @Override
    public List<YearlyCounts> getPublicationCountsForOrganisationUnit(Integer organisationUnitId,
                                                                      Integer startYear,
                                                                      Integer endYear) {
        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId);
        var institutionIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var yearRange = constructYearRange(startYear, endYear, institutionIds, searchFields);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var yearlyCounts = new ArrayList<YearlyCounts>();
        for (int i = yearRange.a; i <= yearRange.b; i++) {
            try {
                var counts =
                    getPublicationCountsByTypeForOUAndYear(institutionIds, i, searchFields);
                if (Objects.nonNull(counts)) {
                    yearlyCounts.add(new YearlyCounts(i, counts));
                }
            } catch (IOException e) {
                log.warn("Unable to fetch OU publication counts for {}. Adding all zeros.", i);
            }
        }

        return yearlyCounts;
    }

    @Override
    public List<MCategoryCounts> getOrganisationUnitPublicationsByMCategories(
        Integer organisationUnitId, Integer startYear, Integer endYear) {
        var result = new ArrayList<MCategoryCounts>();

        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId);
        var commissions = QueryUtil.fetchCommissionsForOrganisationUnit(organisationUnitId);

        var institutionIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        commissions.forEach(commission -> {
            SearchResponse<Void> response;
            try {
                response = elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(0)
                        .query(q -> q
                            .bool(b -> b
                                .must(m -> m.term(t -> t.field("is_approved").value(true)))
                                .must(QueryUtil.organisationUnitMatchQuery(institutionIds,
                                    searchFields))
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
                log.warn("Unable to fetch OU M categories for IDs (OU:{}; C:{}).",
                    organisationUnitId, commission.a);
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
    public List<CommissionYearlyCounts> getMCategoryCountsForOrganisationUnit(
        Integer organisationUnitId, Integer startYear, Integer endYear) {
        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId);
        var institutionIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var yearRange = constructYearRange(startYear, endYear, institutionIds, searchFields);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var commissions = QueryUtil.fetchCommissionsForOrganisationUnit(organisationUnitId);

        var commissionYearlyCounts = new ArrayList<CommissionYearlyCounts>();
        for (var commission : commissions) {
            var yearlyCounts = new ArrayList<YearlyCounts>();

            for (int i = yearRange.a; i <= yearRange.b; i++) {
                try {
                    yearlyCounts.add(new YearlyCounts(i,
                        getPublicationMCategoryCountsByTypeForOUAndCommissionAndYear(
                            institutionIds, commission.a, i, searchFields)));

                } catch (IOException e) {
                    log.warn("Unable to fetch OU M category counts for {}. Adding all zeros.", i);
                }
            }

            commissionYearlyCounts.add(new CommissionYearlyCounts(
                MultilingualContentConverter.getMultilingualContentDTO(commission.b),
                yearlyCounts));
        }

        return commissionYearlyCounts;
    }

    @Override
    public List<StatisticsByCountry> getByCountryStatisticsForOrganisationUnit(
        Integer organisationUnitId, LocalDate from, LocalDate to) {
        var allMergedOrganisationUnitIds =
            QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId);

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("organisation_unit_id").terms(
                                v -> v.value(allMergedOrganisationUnitIds.stream()
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
                            .size(QueryUtil.NUMBER_OF_WORLD_COUNTRIES))
                        .aggregations("country_name", sub -> sub
                            .terms(t -> t.field("country_name").size(1))
                        )
                    ),
                Void.class
            );
        } catch (IOException e) {
            log.warn("Unable to fetch OU statistics for ID {}.", organisationUnitId);
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
    public Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer organisationUnitId,
                                                           LocalDate from, LocalDate to) {
        var allMergedOrganisationUnitIds =
            QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId);

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("organisation_unit_id").terms(
                                v -> v.value(allMergedOrganisationUnitIds.stream()
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
            log.error("Error fetching monthly statistics for OU {} and type VIEW",
                organisationUnitId);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<Year, Long> getYearlyStatisticsCounts(Integer organisationUnitId, Integer startYear,
                                                     Integer endYear) {
        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId);
        var allMergedOrganisationUnitIds =
            QueryUtil.getAllMergedOrganisationUnitIds(organisationUnitId);

        try {
            LocalDate from = LocalDate.of(startYear, 1, 1);
            LocalDate to = LocalDate.of(endYear, 12, 31);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(QueryUtil.organisationUnitMatchQuery(allMergedOrganisationUnitIds,
                                searchFields))
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
            log.error("Error fetching yearly statistics for OU {} and type VIEW",
                organisationUnitId);
            return Collections.emptyMap();
        }
    }

    private Map<String, Long> getPublicationCountsByTypeForOUAndYear(
        List<Integer> organisationUnitIds,
        Integer year,
        List<String> searchFields
    ) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("is_approved").value(true)))
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(QueryUtil.organisationUnitMatchQuery(organisationUnitIds,
                            searchFields))
                        .mustNot(m -> m.term(t -> t.field("type").value("PROCEEDINGS")))
                    )
                )
                .aggregations("by_type", a -> a
                    .terms(t -> t.field("type")
                        .size(DocumentPublicationType.values().length))
                ),
            Void.class
        );

        return response.aggregations()
            .get("by_type")
            .sterms()
            .buckets()
            .array()
            .stream()
            .filter(b -> Objects.nonNull(b.key()))
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                MultiBucketBase::docCount
            ));
    }

    @Override
    public Map<Year, Long> getCitationsByYearForInstitution(Integer institutionId, Integer fromYear,
                                                            Integer toYear) {
        if (Objects.isNull(institutionId) || Objects.isNull(fromYear) || Objects.isNull(toYear)) {
            return Collections.emptyMap();
        }

        var eligibleOUIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(institutionId);

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> {
                var search = s.index("person")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t
                                .field("employment_institutions_id_hierarchy")
                                .terms(ts -> ts.value(
                                    eligibleOUIds.stream().map(FieldValue::of).toList()
                                )))
                            )
                        )
                    );

                for (int year = fromYear; year <= toYear; year++) {
                    int currentYear = year;
                    search.aggregations("year_" + currentYear,
                        a -> a.sum(sum -> sum.field("citations_by_year." + currentYear)));
                }

                return search;
            }, Void.class);

            Map<Year, Long> result = new LinkedHashMap<>();
            for (int year = fromYear; year <= toYear; year++) {
                var agg = response.aggregations().get("year_" + year).sum();
                long value = (agg != null && !Double.isNaN(agg.value())) ? (long) agg.value() : 0L;
                result.put(Year.of(year), value);
            }

            return result;
        } catch (IOException e) {
            log.error("Error fetching citation counts by year for institution {}: {}",
                institutionId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Pair<Integer, Integer> findPublicationYearRange(List<Integer> organisationUnitIds,
                                                            List<String> searchFields) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("is_approved").value(true)))
                            .must(QueryUtil.organisationUnitMatchQuery(organisationUnitIds,
                                searchFields))
                            .must(m -> m.range(t -> t.field("year").gt(JsonData.of(0))))
                        )
                    )
                    .aggregations("earliestYear", a -> a.min(m -> m.field("year")))
                    .aggregations("latestYear", a -> a.max(m -> m.field("year"))),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error finding publication year range for OU with ID {}.",
                organisationUnitIds);
            return new Pair<>(null, null);
        }

        double min = response.aggregations().get("earliestYear").min().value();
        double max = response.aggregations().get("latestYear").max().value();

        return new Pair<>((int) min, (int) max);
    }

    private Map<String, Long> getPublicationMCategoryCountsByTypeForOUAndCommissionAndYear(
        List<Integer> organisationUnitIds, Integer commissionId, Integer year,
        List<String> searchFields)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("is_approved").value(true)))
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(QueryUtil.organisationUnitMatchQuery(organisationUnitIds,
                            searchFields))
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

    private Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear,
                                                      List<Integer> organisationUnitIds,
                                                      List<String> searchFields) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var yearRange = findPublicationYearRange(organisationUnitIds, searchFields);
            return new Pair<>(yearRange.a, yearRange.b);
        }

        return new Pair<>(startYear, endYear);
    }
}
