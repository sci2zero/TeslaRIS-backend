package rs.teslaris.revisioner.util.dataquality;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Selects the assessment current at an instant, or the latest one when no instant is given.
 */
public final class PointInTimeQueries {

    private static final DateTimeFormatter INSTANT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private PointInTimeQueries() {
    }

    public static Query assessmentsValidOn(@Nullable LocalDate day) {
        return Objects.isNull(day) ? latestAssessments() : assessmentsValidAtEndOf(day);
    }

    public static Query latestAssessments() {
        return TermQuery.of(t -> t.field("is_latest").value(true))._toQuery();
    }

    // The last millisecond of the day, so the newest of several same-day assessments wins.
    public static Query assessmentsValidAtEndOf(LocalDate day) {
        var instant = INSTANT_FORMAT.format(
            LocalDateTime.ofInstant(day.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC));

        return BoolQuery.of(b -> b
            .must(RangeQuery.of(r -> r.field("assessment_date").lte(JsonData.of(instant)))
                ._toQuery())
            .must(RangeQuery.of(r -> r.field("valid_to").gt(JsonData.of(instant)))._toQuery())
        )._toQuery();
    }
}
