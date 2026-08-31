package rs.teslaris.revisioner.util.dataquality;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityAggregator {

    private static final String ASSESSMENT_INDEX = "data_quality_assessment";

    private static final String DOCUMENT_INDEX = "document_publication";

    private static final String ACTIVITY_OCCURRENCES_FIELD = "activity_issue_occurrences";

    private final ElasticsearchClient elasticsearchClient;


    public Optional<AssessmentAggregates> aggregateAssessments(Query query,
                                                               Collection<String> activityRuleKeys) {
        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(true))
            .query(query)
            .aggregations("errorFailures", a -> a.sum(s -> s.field("error_failed_rules")))
            .aggregations("warningFailures", a -> a.sum(s -> s.field("warning_failed_rules")))
            .aggregations("infoFailures", a -> a.sum(s -> s.field("info_failed_rules")))
            .aggregations("activities", a -> a.sum(s -> s.field("activities_count")))
            .aggregations("activityCandidates", a -> a
                .sum(s -> s.field("activity_publication_candidates_count")))
            .aggregations("activityScore", a -> a.sum(s -> s.field("activity_score_sum")))
            .aggregations("averageScore", a -> a.avg(avg -> avg.field("quality_score")))
            .aggregations("publicationCandidates", a -> a
                .filter(f -> f.term(term -> term.field("publication_candidate").value(true))));

        if (!activityRuleKeys.isEmpty()) {
            request.aggregations("activityIssues", a -> a
                .terms(terms -> terms
                    .field("failed_rule_keys")
                    .include(include -> include.terms(List.copyOf(activityRuleKeys)))
                    .size(activityRuleKeys.size())
                    .minDocCount(1)));
        }

        try {
            var response = elasticsearchClient.search(request.build(), Void.class);

            if (Objects.isNull(response)) {
                return Optional.empty();
            }

            var affectedRecords = totalHits(response.hits().total());

            return Optional.of(new AssessmentAggregates(
                affectedRecords,
                sum(response, "errorFailures") + sum(response, "warningFailures") +
                    sum(response, "infoFailures"),
                sum(response, "activities"),
                bucketTotal(response, "activityIssues"),
                sum(response, "activityCandidates"),
                sumAsDouble(response, "activityScore"),
                filterTotal(response, "publicationCandidates"),
                affectedRecords > 0 ? average(response) : null
            ));
        } catch (Exception e) {
            log.warn("Unable to aggregate related assessments. Reason: {}", e.getMessage());

            return Optional.empty();
        }
    }

    public Optional<LinkedDocumentAggregates> aggregateLinkedDocuments(Query query) {
        var request = new SearchRequest.Builder()
            .index(DOCUMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(true))
            .query(query)
            .aggregations("activities", a -> a.sum(s -> s.field("activities_count")))
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            if (Objects.isNull(response)) {
                return Optional.empty();
            }

            return Optional.of(new LinkedDocumentAggregates(
                totalHits(response.hits().total()),
                sum(response, "activities")
            ));
        } catch (Exception e) {
            log.warn("Unable to aggregate linked documents. Reason: {}", e.getMessage());

            return Optional.empty();
        }
    }

    /**
     * Every dimension keeps its figures in flat fields named after it, so one request answers
     * all of them: an average of the score, a sum of the issue count, and a count of the records
     * whose issue count is above zero.
     */
    public Optional<Map<QualityDimension, DimensionAggregates>> aggregateDimensions(Query query) {
        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query);

        for (var dimension : QualityDimension.values()) {
            var prefix = dimension.name().toLowerCase();

            request.aggregations(prefix + "Score", a -> a.avg(avg -> avg.field(prefix + "_score")));
            request.aggregations(prefix + "Issues",
                a -> a.sum(sum -> sum.field(prefix + "_issue_count")));
            request.aggregations(prefix + "Affected", a -> a
                .filter(f -> f.range(range -> range
                    .field(prefix + "_issue_count")
                    .gt(JsonData.of(0)))));
        }

        try {
            var response = elasticsearchClient.search(request.build(), Void.class);

            if (Objects.isNull(response)) {
                return Optional.empty();
            }

            var aggregates = new EnumMap<QualityDimension, DimensionAggregates>(
                QualityDimension.class);

            for (var dimension : QualityDimension.values()) {
                var prefix = dimension.name().toLowerCase();

                aggregates.put(dimension, new DimensionAggregates(
                    average(response, prefix + "Score"),
                    sum(response, prefix + "Issues"),
                    filterTotal(response, prefix + "Affected")
                ));
            }

            return Optional.of(aggregates);
        } catch (Exception e) {
            log.warn("Unable to aggregate quality dimensions. Reason: {}", e.getMessage());

            return Optional.empty();
        }
    }

    /**
     * The rule that failed most often within a family, decided in the shards from the keyword
     * array's ordinals, the bucket count is bounded by the profile.
     * <p>
     * Ties are broken by rule key ascending, done here rather than with an aggregation order so the
     * result does not depend on how the client spells composite ordering.
     */
    public Optional<TopFailedRule> topFailedRule(Query query, Collection<String> ruleKeys) {
        return topFailedRule(query, ruleKeys, "failed_rule_keys");
    }

    /**
     * The Activity rule affecting the most activities, rather than the most records.
     * <p>
     * {@code topFailedRule} aggregates a distinct-key list, so its bucket counts are records. The
     * per-rule occurrence counters have to be summed field by field instead, which is one
     * aggregation per rule key - affordable because the Activity family is small and every sum
     * reads doc_values of a field only this report touches.
     * <p>
     * Ties break on the alphabetically first key, matching {@code topFailedRule}.
     */
    public Optional<TopFailedRule> topRuleByActivityOccurrences(Query query,
                                                                Collection<String> ruleKeys) {
        if (ruleKeys.isEmpty()) {
            return Optional.empty();
        }

        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query);

        ruleKeys.forEach(ruleKey -> request.aggregations(ruleKey, a -> a
            .sum(s -> s.field(ACTIVITY_OCCURRENCES_FIELD + "." + ruleKey))));

        try {
            var response = elasticsearchClient.search(request.build(), Void.class);

            if (Objects.isNull(response)) {
                return Optional.empty();
            }

            return ruleKeys.stream()
                .map(ruleKey -> new TopFailedRule(ruleKey, sum(response, ruleKey)))
                .filter(rule -> rule.occurrences() > 0)
                .max(Comparator
                    .comparingLong(TopFailedRule::occurrences)
                    .thenComparing(TopFailedRule::ruleKey, Comparator.reverseOrder()));
        } catch (Exception e) {
            log.warn("Unable to aggregate the most frequent activity rule. Reason: {}",
                e.getMessage());

            return Optional.empty();
        }
    }

    public Optional<TopFailedRule> topFailedRule(Query query, Collection<String> ruleKeys,
                                                 String field) {
        if (ruleKeys.isEmpty()) {
            return Optional.empty();
        }

        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query)
            .aggregations("topRules", a -> a
                .terms(terms -> terms
                    .field(field)
                    .include(include -> include.terms(List.copyOf(ruleKeys)))
                    .size(ruleKeys.size())
                    .minDocCount(1)))
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            if (Objects.isNull(response) ||
                Objects.isNull(response.aggregations().get("topRules"))) {
                return Optional.empty();
            }

            return response.aggregations().get("topRules").sterms().buckets().array().stream()
                .max(Comparator
                    .comparingLong(StringTermsBucket::docCount)
                    .thenComparing(bucket -> bucket.key().stringValue(),
                        Comparator.reverseOrder()))
                .map(bucket -> new TopFailedRule(bucket.key().stringValue(), bucket.docCount()));
        } catch (Exception e) {
            log.warn("Unable to aggregate the most frequent failed rule. Reason: {}",
                e.getMessage());

            return Optional.empty();
        }
    }

    /**
     * How many distinct constraints block publication across the matched records, and how many
     * blocking failures there are in total. The cardinality is exact here: the profile bounds the
     * key space far below the aggregation's precision threshold.
     */
    public Optional<BlockingAggregates> aggregateBlocking(Query query) {
        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query)
            .aggregations("distinctBlockingRules",
                a -> a.cardinality(c -> c.field("blocking_rule_keys")))
            .aggregations("blockingFailures", a -> a.sum(s -> s.field("blocking_failed_rules")))
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            if (Objects.isNull(response)) {
                return Optional.empty();
            }

            var distinct = response.aggregations().get("distinctBlockingRules");

            return Optional.of(new BlockingAggregates(
                Objects.isNull(distinct) ? 0 : distinct.cardinality().value(),
                sum(response, "blockingFailures")
            ));
        } catch (Exception e) {
            log.warn("Unable to aggregate blocking constraints. Reason: {}", e.getMessage());

            return Optional.empty();
        }
    }

    /**
     * Counts the records of any index the given query matches, without transferring them.
     */
    public long countRecords(String indexName, Query query) {
        var request = new SearchRequest.Builder()
            .index(indexName)
            .size(0)
            .trackTotalHits(total -> total.enabled(true))
            .query(query)
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            return Objects.isNull(response) ? 0 : totalHits(response.hits().total());
        } catch (Exception e) {
            log.warn("Unable to count records of index {}. Reason: {}", indexName, e.getMessage());

            return 0;
        }
    }

    /**
     * Sums a numeric field over the records of any index the given query matches. Used for activity
     * counters, which live on the records that carry the activities rather than on records of their
     * own.
     */
    public long sumField(String indexName, Query query, String field) {
        var request = new SearchRequest.Builder()
            .index(indexName)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query)
            .aggregations("fieldSum", a -> a.sum(s -> s.field(field)))
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            return Objects.isNull(response) ? 0 : sum(response, "fieldSum");
        } catch (Exception e) {
            log.warn("Unable to sum field {} of index {}. Reason: {}", field, indexName,
                e.getMessage());

            return 0;
        }
    }

    /**
     * One document failing one rule is exactly one issue row, so summing the buckets of a terms
     * aggregation restricted to the applicable rule keys yields the issue total without fetching a
     * single document and without the result window that caps a document scan.
     */
    public OptionalLong countIssues(Query query, Collection<String> ruleKeys) {
        if (ruleKeys.isEmpty()) {
            return OptionalLong.of(0);
        }

        var request = new SearchRequest.Builder()
            .index(ASSESSMENT_INDEX)
            .size(0)
            .trackTotalHits(total -> total.enabled(false))
            .query(query)
            .aggregations("issueCounts", a -> a
                .terms(terms -> terms
                    .field("failed_rule_keys")
                    .include(include -> include.terms(List.copyOf(ruleKeys)))
                    .size(ruleKeys.size())
                    .minDocCount(1)))
            .build();

        try {
            var response = elasticsearchClient.search(request, Void.class);

            if (Objects.isNull(response) ||
                Objects.isNull(response.aggregations().get("issueCounts"))) {
                return OptionalLong.empty();
            }

            return OptionalLong.of(bucketTotal(response, "issueCounts"));
        } catch (Exception e) {
            log.warn("Unable to aggregate issue count. Reason: {}", e.getMessage());

            return OptionalLong.empty();
        }
    }

    private long totalHits(co.elastic.clients.elasticsearch.core.search.TotalHits totalHits) {
        return Objects.isNull(totalHits) ? 0 : totalHits.value();
    }

    private double sumAsDouble(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                               String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        if (Objects.isNull(aggregate)) {
            return 0.0;
        }

        var value = aggregate.sum().value();

        return Double.isNaN(value) ? 0.0 : value;
    }

    private long sum(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                     String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        if (Objects.isNull(aggregate)) {
            return 0;
        }

        var value = aggregate.sum().value();

        return Double.isNaN(value) ? 0 : (long) value;
    }

    private long filterTotal(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                             String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        return Objects.isNull(aggregate) ? 0 : aggregate.filter().docCount();
    }

    private long bucketTotal(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                             String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        if (Objects.isNull(aggregate)) {
            return 0;
        }

        return aggregate.sterms().buckets().array().stream()
            .mapToLong(MultiBucketBase::docCount)
            .sum();
    }

    private Double average(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response) {
        return average(response, "averageScore");
    }

    private Double average(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                           String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        if (Objects.isNull(aggregate)) {
            return null;
        }

        var value = aggregate.avg().value();

        return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
    }

    public record AssessmentAggregates(long affectedRecords, long openIssues, long activitiesCount,
                                       long activityIssues, long activityPublicationCandidates,
                                       double activityScoreSum, long publicationCandidates,
                                       Double averageScore) {
        public static AssessmentAggregates empty() {
            return new AssessmentAggregates(
                0,
                0,
                0,
                0,
                0,
                0.0,
                0,
                null
            );
        }
    }

    public record TopFailedRule(String ruleKey, long occurrences) {
    }

    public record BlockingAggregates(long distinctBlockingConstraints, long blockingIssues) {

        public static BlockingAggregates empty() {
            return new BlockingAggregates(0, 0);
        }
    }

    public record DimensionAggregates(@Nullable Double averageScore, long openIssues,
                                      long affectedRecords) {
    }

    public record LinkedDocumentAggregates(long linkedRecords, long linkedActivities) {

        public static LinkedDocumentAggregates empty() {
            return new LinkedDocumentAggregates(0, 0);
        }
    }
}
