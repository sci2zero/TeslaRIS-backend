package rs.teslaris.reporting.service.impl.visualizations;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.service.interfaces.visualizations.DocumentVisualizationDataService;
import rs.teslaris.reporting.utility.QueryUtil;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class DocumentVisualizationDataServiceImpl implements DocumentVisualizationDataService {

    private final ElasticsearchClient elasticsearchClient;

    private final DocumentPublicationService documentPublicationService;

    private final SearchService<DocumentPublicationIndex> searchService;


    @Override
    public List<StatisticsByCountry> getByCountryStatisticsForDocument(Integer documentId,
                                                                       LocalDate from, LocalDate to,
                                                                       StatisticsType statisticsType) {
        var allMergedDocumentIds = getAllMergedDocumentIds(documentId);

        SearchResponse<Void> response;
        try {
            response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("document_id").terms(
                                v -> v.value(allMergedDocumentIds.stream()
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
                            .size(
                                195)) // 195 countries exist at the moment, we can lower this if need be
                        .aggregations("country_name", sub -> sub
                            .terms(t -> t.field("country_name").size(1))
                        )
                    ),
                Void.class
            );
        } catch (IOException e) {
            log.warn("Unable to fetch document {} statistics for ID {}.", statisticsType,
                documentId);
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
    public Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer documentId, LocalDate from,
                                                           LocalDate to,
                                                           StatisticsType statisticsType) {
        var allMergedDocumentIds = getAllMergedDocumentIds(documentId);

        try {
            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("document_id").terms(
                                v -> v.value(allMergedDocumentIds.stream()
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
                    .aggregations("per_month", a -> a
                        .dateHistogram(h -> h
                            .field("timestamp")
                            .calendarInterval(CalendarInterval.Month)
                            .minDocCount(0)
                            .extendedBounds(b -> b
                                .min(FieldDateMath.of(
                                    fdm -> fdm.expr(from.toString().substring(0, 7))))
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
            log.error("Error fetching monthly statistics for document {} and type {}", documentId,
                statisticsType.name());
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<Year, Long> getYearlyStatisticsCounts(Integer documentId, Integer startYear,
                                                     Integer endYear,
                                                     StatisticsType statisticsType) {
        var allMergedDocumentIds = getAllMergedDocumentIds(documentId);

        try {
            LocalDate from = LocalDate.of(startYear, 1, 1);
            LocalDate to = LocalDate.of(endYear, 12, 31);

            SearchResponse<Void> response = elasticsearchClient.search(s -> s
                    .index("statistics")
                    .size(0)
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.terms(t -> t.field("document_id").terms(
                                v -> v.value(allMergedDocumentIds.stream()
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
            log.error("Error fetching yearly statistics for document {} and type {}", documentId,
                statisticsType.name());
            return Collections.emptyMap();
        }
    }

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForTypeAndPeriod(
        DocumentPublicationType type,
        Integer yearFrom,
        Integer yearTo,
        Integer personId,
        Integer institutionId,
        Pageable pageable) {
        var searchQuery = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.must(buildPublicationMetadataQuery(type, yearFrom, yearTo, personId, institutionId));
            return b;
        })))._toQuery();

        return searchService.runQuery(searchQuery, pageable, DocumentPublicationIndex.class,
            "document_publication");
    }

    private Query buildPublicationMetadataQuery(DocumentPublicationType type, Integer yearFrom,
                                                Integer yearTo, Integer personId,
                                                Integer institutionId) {
        if ((Objects.isNull(personId) || personId < 0) &&
            (Objects.isNull(institutionId) || institutionId < 0)) {
            throw new IllegalArgumentException("Both person and institution IDs cannot be null.");
        }

        return BoolQuery.of(b -> {
            if (Objects.nonNull(personId) && personId > 0) {
                b.must(q -> q.term(t -> t.field("author_ids").value(personId)));
            } else {
                var searchFields = QueryUtil.getOrganisationUnitOutputSearchFields(institutionId);
                b.must(QueryUtil.organisationUnitMatchQuery(List.of(institutionId), searchFields));
            }

            b.must(q -> q.term(t -> t.field("type").value(type.name())));

            b.must(m -> m.range(
                    r -> r.field("year")
                        .gte(JsonData.of(yearFrom))
                        .lte(JsonData.of(yearTo))
                )
            );

            return b;
        })._toQuery();
    }

    private List<Integer> getAllMergedDocumentIds(Integer documentId) {
        var allMergedDocumentIds = new ArrayList<>(List.of(documentId));
        allMergedDocumentIds.addAll(documentPublicationService.findOne(documentId).getMergedIds());

        return allMergedDocumentIds;
    }
}
