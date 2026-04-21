package rs.teslaris.core.applicationevent;

import rs.teslaris.core.model.commontypes.ApplicableEntityType;

public record EntityAssessmentChanged(
    ApplicableEntityType entityType,
    Integer entityId,
    Integer commissionId,
    Boolean deleted
) {
}
