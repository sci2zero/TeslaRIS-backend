package rs.teslaris.revisioner.model;

public record DataQualityAssessmentEvent(
    EntityRevision entityRevision,
    String json
) {
}
