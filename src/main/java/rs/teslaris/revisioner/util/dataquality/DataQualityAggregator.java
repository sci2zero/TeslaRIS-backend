package rs.teslaris.revisioner.util.dataquality;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityAggregator {

    private static final String ASSESSMENT_INDEX = "data_quality_assessment";

    private static final String DOCUMENT_INDEX = "document_publication";

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
            .aggregations("averageScore", a -> a.avg(avg -> avg.field("quality_score")));

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

    private long sum(co.elastic.clients.elasticsearch.core.SearchResponse<Void> response,
                     String aggregationName) {
        var aggregate = response.aggregations().get(aggregationName);

        if (Objects.isNull(aggregate)) {
            return 0;
        }

        var value = aggregate.sum().value();

        return Double.isNaN(value) ? 0 : (long) value;
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
        var aggregate = response.aggregations().get("averageScore");

        if (Objects.isNull(aggregate)) {
            return null;
        }

        var value = aggregate.avg().value();

        return Double.isNaN(value) || Double.isInfinite(value) ? null : value;
    }

    public record AssessmentAggregates(long affectedRecords, long openIssues, long activitiesCount,
                                       long activityIssues, Double averageScore) {

        public static AssessmentAggregates empty() {
            return new AssessmentAggregates(0, 0, 0, 0, null);
        }
    }

    public record LinkedDocumentAggregates(long linkedRecords, long linkedActivities) {

        public static LinkedDocumentAggregates empty() {
            return new LinkedDocumentAggregates(0, 0);
        }
    }
}
