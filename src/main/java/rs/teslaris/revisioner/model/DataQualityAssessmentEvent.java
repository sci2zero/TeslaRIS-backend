package rs.teslaris.revisioner.model;

import jakarta.annotation.Nullable;

/**
 * @param profileName the only profile to assess, or {@code null} to assess against every configured
 *                    profile
 */
public record DataQualityAssessmentEvent(
    EntityRevision entityRevision,
    String json,

    @Nullable
    String profileName
) {

    public DataQualityAssessmentEvent(EntityRevision entityRevision, String json) {
        this(entityRevision, json, null);
    }
}
