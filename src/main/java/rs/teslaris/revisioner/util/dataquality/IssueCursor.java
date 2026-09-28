package rs.teslaris.revisioner.util.dataquality;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.Objects;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;

/**
 * Names one row of the issues listing so a later request can resume right after it.
 */
public record IssueCursor(
    Integer entityId,
    IssueSeverity severity,
    String ruleKey,
    String entityType,
    Integer assessmentId
) {

    // Entity type first, because one entity id is shared across types and its rows would otherwise
    // interleave; then most severe first, with the assessment id keeping the order total under two
    // current assessments.
    public static final Comparator<IssueCursor> WITHIN_RECORD = Comparator
        .comparing(IssueCursor::entityType)
        .thenComparingInt(cursor -> -cursor.severity().ordinal())
        .thenComparing(IssueCursor::ruleKey)
        .thenComparing(IssueCursor::assessmentId);

    private static final String SEPARATOR = "\u001F";

    private static final int FIELD_COUNT = 5;

    public static IssueCursor decode(String encoded) {
        if (Objects.isNull(encoded) || encoded.isBlank()) {
            throw new IllegalArgumentException("Cursor must not be empty.");
        }

        try {
            var payload =
                new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            var fields = payload.split(SEPARATOR, -1);

            if (fields.length != FIELD_COUNT) {
                throw new IllegalArgumentException("Cursor is not valid.");
            }

            return new IssueCursor(
                Integer.valueOf(fields[0]),
                IssueSeverity.valueOf(fields[1]),
                fields[2],
                fields[3],
                Integer.valueOf(fields[4]));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Cursor is not valid.", e);
        }
    }

    public String encode() {
        var payload = String.join(SEPARATOR,
            String.valueOf(entityId), severity.name(), ruleKey, entityType,
            String.valueOf(assessmentId));

        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
}
