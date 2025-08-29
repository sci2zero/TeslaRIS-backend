package rs.teslaris.reporting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitOutputConfigurationService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.OrganisationUnitVisualizationDataService;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationUnitVisualizationDataServiceImpl implements
    OrganisationUnitVisualizationDataService {

    private final ElasticsearchClient elasticsearchClient;

    private final OrganisationUnitService organisationUnitService;

    private final UserService userService;

    private final OrganisationUnitOutputConfigurationService
        organisationUnitOutputConfigurationService;


    @Override
    public List<YearlyCounts> getPublicationCountsForOrganisationUnit(Integer organisationUnitId,
                                                                      Integer startYear,
                                                                      Integer endYear) {
        var searchFields = getOrganisationUnitOutputSearchFields(organisationUnitId);
        var yearRange = constructYearRange(startYear, endYear, organisationUnitId, searchFields);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var yearlyCounts = new ArrayList<YearlyCounts>();
        for (int i = yearRange.a; i <= yearRange.b; i++) {
            try {
                var counts =
                    getPublicationCountsByTypeForOUAndYear(organisationUnitId, i, searchFields);
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

        var searchFields = getOrganisationUnitOutputSearchFields(organisationUnitId);
        var commissions = fetchCommissionsForOrganisationUnit(organisationUnitId);

        commissions.forEach(commission -> {
            SearchResponse<Void> response;
            try {
                response = elasticsearchClient.search(s -> s
                        .index("document_publication")
                        .size(0)
                        .query(q -> q
                            .bool(b -> b
                                .must(organisationUnitMatchQuery(List.of(organisationUnitId),
                                    searchFields))
                                .must(m -> m.range(
                                    t -> t.field("year").gte(JsonData.of(startYear))
                                        .lte(JsonData.of(endYear))))
                                .must(m -> m.bool(sb -> sb
                                    .should(sn -> sn.term(
                                        t -> t.field("commission_assessments.a").value(commission.a)))
                                    .should(sn -> sn.bool(nb -> nb.mustNot(
                                        mn -> mn.exists(e -> e.field("commission_assessments.a")))))
                                    .minimumShouldMatch("1")
                                ))
                            )
                        )
                        .aggregations("by_m_category", a -> a
                            .terms(t -> t.field("commission_assessments.b.keyword")
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
        var searchFields = getOrganisationUnitOutputSearchFields(organisationUnitId);

        var yearRange = constructYearRange(startYear, endYear, organisationUnitId, searchFields);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b)) {
            return Collections.emptyList();
        }

        var commissions = fetchCommissionsForOrganisationUnit(organisationUnitId);

        var commissionYearlyCounts = new ArrayList<CommissionYearlyCounts>();
        for (var commission : commissions) {
            var yearlyCounts = new ArrayList<YearlyCounts>();

            for (int i = yearRange.a; i <= yearRange.b; i++) {
                try {
                    yearlyCounts.add(new YearlyCounts(i,
                        getPublicationMCategoryCountsByTypeForOUAndCommissionAndYear(
                            organisationUnitId, commission.a, i, searchFields)));

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
        var searchFields = getOrganisationUnitOutputSearchFields(organisationUnitId);
        var allMergedOrganisationUnitIds = getAllMergedOrganisationUnitIds(organisationUnitId);

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(
                                organisationUnitMatchQuery(allMergedOrganisationUnitIds, searchFields))
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
        var searchFields = getOrganisationUnitOutputSearchFields(organisationUnitId);
        var allMergedOrganisationUnitIds = getAllMergedOrganisationUnitIds(organisationUnitId);

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(
                                organisationUnitMatchQuery(allMergedOrganisationUnitIds, searchFields))
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

    private Map<String, Long> getPublicationCountsByTypeForOUAndYear(
        Integer organisationUnitId,
        Integer year,
        List<String> searchFields
    ) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(organisationUnitMatchQuery(List.of(organisationUnitId), searchFields))
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
            .filter(b -> Objects.nonNull(b.key()))
            .collect(Collectors.toMap(
                b -> b.key().stringValue(),
                MultiBucketBase::docCount
            ));
    }

    private List<String> getOrganisationUnitOutputSearchFields(Integer organisationUnitId) {
        var fields = new ArrayList<String>();

        var outputConfiguration =
            organisationUnitOutputConfigurationService.readOutputConfigurationForOrganisationUnit(
                organisationUnitId);

        if (!outputConfiguration.showOutputs()) {
            return Collections.emptyList();
        }

        if (outputConfiguration.showBySpecifiedAffiliation()) {
            fields.add("organisation_unit_ids_specified");
        }

        if (outputConfiguration.showByPublicationYearEmployments()) {
            fields.add("organisation_unit_ids_year_of_publication");
        }

        if (outputConfiguration.showByCurrentEmployments()) {
            fields.add("organisation_unit_ids_active");
        }

        return fields;
    }

    private Pair<Integer, Integer> findPublicationYearRange(Integer organisationUnitId,
                                                            List<String> searchFields) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(organisationUnitMatchQuery(List.of(organisationUnitId), searchFields))
                            .must(m -> m.range(t -> t.field("year").gt(JsonData.of(0))))
                        )
                    )
                    .aggregations("earliestYear", a -> a.min(m -> m.field("year")))
                    .aggregations("latestYear", a -> a.max(m -> m.field("year"))),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error finding publication year range for OU with ID {}.",
                organisationUnitId);
            return new Pair<>(null, null);
        }

        double min = response.aggregations().get("earliestYear").min().value();
        double max = response.aggregations().get("latestYear").max().value();

        return new Pair<>((int) min, (int) max);
    }

    private Map<String, Long> getPublicationMCategoryCountsByTypeForOUAndCommissionAndYear(
        Integer organisationUnitId, Integer commissionId, Integer year, List<String> searchFields)
        throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m.term(t -> t.field("year").value(year)))
                        .must(organisationUnitMatchQuery(List.of(organisationUnitId), searchFields))
                        .must(m -> m.bool(sb -> sb
                            .should(sn -> sn.term(
                                t -> t.field("commission_assessments.a").value(commissionId)))
                            .should(sn -> sn.bool(nb -> nb.mustNot(
                                mn -> mn.exists(e -> e.field("commission_assessments.a")))))
                            .minimumShouldMatch("1")
                        ))
                    )
                )
                .aggregations("by_m_category", a -> a
                    .terms(t -> t.field("commission_assessments.b.keyword")
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
                                                      Integer organisationUnitId,
                                                      List<String> searchFields) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var yearRange = findPublicationYearRange(organisationUnitId, searchFields);
            return new Pair<>(yearRange.a, yearRange.b);
        }

        return new Pair<>(startYear, endYear);
    }

    private Query organisationUnitMatchQuery(List<Integer> organisationUnitIds,
                                             List<String> searchFields) {
        return Query.of(q -> q.bool(b -> {
            for (String field : searchFields) {
                for (Integer id : organisationUnitIds) {
                    b.should(s -> s.term(t -> t.field(field).value(id)));
                }
            }
            return b.minimumShouldMatch("1");
        }));
    }

    private Set<Pair<Integer, Set<MultiLingualContent>>> fetchCommissionsForOrganisationUnit(
        Integer organisationUnitId) {
        var commissions = new HashSet<Pair<Integer, Set<MultiLingualContent>>>();
        userService.findCommissionForOrganisationUnitId(organisationUnitId).forEach(commission -> {
            commissions.add(new Pair<>(commission.getId(), commission.getDescription()));
        });

        return commissions;
    }

    private List<Integer> getAllMergedOrganisationUnitIds(Integer organisationUnitId) {
        var result = new ArrayList<>(List.of(organisationUnitId));
        result.addAll(organisationUnitService.findOne(organisationUnitId).getMergedIds());

        return result;
    }
}
