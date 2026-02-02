package rs.teslaris.core.applicationevent;

import rs.teslaris.core.indexmodel.DocumentPublicationType;

public record ReassessEntityEvent(
    DocumentPublicationType documentPublicationType,
    Integer entityId
) {
}
