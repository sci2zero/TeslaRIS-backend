package rs.teslaris.core.applicationevent;

import rs.teslaris.assessment.model.indicator.ApplicableEntityType;

public record EntityAssessmentChanged(
    ApplicableEntityType entityType,
    Integer entityId,
    Integer commissionId,
    Boolean deleted
) {
}
