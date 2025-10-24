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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.leaderboards.DocumentLeaderboardService;
import rs.teslaris.reporting.service.interfaces.visualizations.DigitalLibraryVisualizationDataService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalLibraryVisualizationDataServiceImpl
    implements DigitalLibraryVisualizationDataService {


    private final ElasticsearchClient elasticsearchClient;

    private final DocumentLeaderboardService documentLeaderboardService;

    private final OrganisationUnitService organisationUnitService;


    @Override
    public List<YearlyCounts> getThesisCountsForOrganisationUnit(Integer organisationUnitId,
                                                                 Integer startYear,
                                                                 Integer endYear,
                                                                 List<ThesisType> allowedThesisTypes) {
        var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(organisationUnitId);
        var institutionIds =
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var yearRange = constructYearRange(startYear, endYear, institutionIds, searchFields,
            allowedThesisTypes);
        if (Objects.isNull(yearRange.a) || Objects.isNull(yearRange.b) ||
            !organisationUnitService.findOne(organisationUnitId).getIsClientInstitutionDl()) {
            return Collections.emptyList();
        }

        var yearlyCounts = new ArrayList<YearlyCounts>();
        for (int i = yearRange.a; i <= yearRange.b; i++) {
            try {
                var counts =
                    getThesisCountsByTypeForOUAndYearAndAllowedTypes(institutionIds, i,
                        searchFields, allowedThesisTypes);
                if (Objects.nonNull(counts)) {
                    yearlyCounts.add(new YearlyCounts(i, counts));
                }
            } catch (IOException e) {
                log.warn("Unable to fetch OU theses counts for {}. Adding all zeros.", i);
            }
        }

        return yearlyCounts;
    }

    @Override
    public Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer organisationUnitId,
                                                           LocalDate from, LocalDate to,
                                                           StatisticsType statisticsType,
                                                           List<ThesisType> allowedThesisTypes) {
        var eligibleDocumentIds =
            documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
                allowedThesisTypes);
        if (eligibleDocumentIds.isEmpty() ||
            !organisationUnitService.findOne(organisationUnitId).getIsClientInstitutionDl()) {
            return Collections.emptyMap();
        }

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("document_id").terms(ts -> ts
                                .value(eligibleDocumentIds.stream().map(FieldValue::of).toList()))))
                            .must(m -> m.term(t -> t.field("is_bot").value(false)))
                            .must(m -> m.term(t -> t.field("type").value(statisticsType.name())))
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
            log.error("Error fetching monthly thesis statistics for OU {} and type {}. Reason: {}",
                organisationUnitId, statisticsType.name(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<StatisticsByCountry> getByCountryStatisticsForDigitalLibrary(
        Integer organisationUnitId, LocalDate from, LocalDate to, StatisticsType statisticsType,
        List<ThesisType> allowedThesisTypes) {
        var eligibleDocumentIds =
            documentLeaderboardService.getEligibleDocumentIds(organisationUnitId, true,
                allowedThesisTypes);
        if (eligibleDocumentIds.isEmpty() ||
            !organisationUnitService.findOne(organisationUnitId).getIsClientInstitutionDl()) {
            return Collections.emptyList();
        }

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("document_id").terms(
                                v -> v.value(eligibleDocumentIds.stream()
                                    .map(FieldValue::of)
                                    .toList())
                            )))
                            .must(m -> m.term(t -> t.field("is_bot").value(false)))
                            .must(m -> m.term(t -> t.field("type").value(statisticsType.name())))
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
            log.warn("Unable to fetch DL {} statistics for institution {}.", statisticsType,
                organisationUnitId);
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

    private Pair<Integer, Integer> constructYearRange(Integer startYear, Integer endYear,
                                                      List<Integer> organisationUnitIds,
                                                      List<String> searchFields,
                                                      List<ThesisType> allowedThesisTypes) {
        if (Objects.isNull(startYear) || Objects.isNull(endYear)) {
            var yearRange =
                findPublicationYearRange(organisationUnitIds, searchFields, allowedThesisTypes);
            return new Pair<>(yearRange.a, yearRange.b);
        }

        return new Pair<>(startYear, endYear);
    }

    private Pair<Integer, Integer> findPublicationYearRange(List<Integer> organisationUnitIds,
                                                            List<String> searchFields,
                                                            List<ThesisType> allowedThesisTypes) {
        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("document_publication")
                    .size(0)
                    .query(q -> q
                        .bool(b -> {
                                b.must(m -> m.term(t -> t.field("is_approved").value(true)))
                                    .must(m -> m.term(t -> t.field("type").value("THESIS")))
                                    .must(QueryUtil.organisationUnitMatchQuery(organisationUnitIds,
                                        searchFields))
                                    .must(m -> m.range(t -> t.field("year").gt(JsonData.of(0))));

                                QueryUtil.applyAllowedThesisTypesFilter(b, allowedThesisTypes);

                                return b;
                            }
                        )
                    )
                    .aggregations("earliestYear", a -> a.min(m -> m.field("year")))
                    .aggregations("latestYear", a -> a.max(m -> m.field("year"))),
                Void.class
            );
        } catch (IOException e) {
            log.error("Error finding publication year range for thesis from OU with ID {}.",
                organisationUnitIds);
            return new Pair<>(null, null);
        }

        double min = response.aggregations().get("earliestYear").min().value();
        double max = response.aggregations().get("latestYear").max().value();

        return new Pair<>((int) min, (int) max);
    }

    private Map<String, Long> getThesisCountsByTypeForOUAndYearAndAllowedTypes(
        List<Integer> organisationUnitIds,
        Integer year,
        List<String> searchFields,
        List<ThesisType> allowedThesisTypes
    ) throws IOException {
        SearchResponse<Void> response = elasticsearchClient.search(s -> s
                .index("document_publication")
                .size(0)
                .query(q -> q
                    .bool(b -> {
                            b.must(m -> m.term(t -> t.field("is_approved").value(true)))
                                .must(m -> m.term(t -> t.field("year").value(year)))
                                .must(QueryUtil.organisationUnitMatchQuery(organisationUnitIds,
                                    searchFields))
                                .must(m -> m.term(t -> t.field("type").value("THESIS")));

                            QueryUtil.applyAllowedThesisTypesFilter(b, allowedThesisTypes);

                            return b;
                        }
                    )
                )
                .aggregations("by_publication_type", a -> a
                    .terms(t -> t.field("publication_type").size(getBucketSize(allowedThesisTypes)))
                ),
            Void.class
        );

        return response.aggregations()
            .get("by_publication_type")
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

    private int getBucketSize(List<ThesisType> allowedThesisTypes) {
        if (Objects.isNull(allowedThesisTypes) || allowedThesisTypes.isEmpty()) {
            return 7; // for 7 thesis types
        }

        return allowedThesisTypes.size();
    }
}
