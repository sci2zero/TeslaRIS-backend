package rs.teslaris.core.applicationevent;

public record PublicVisibilityChangedEvent(
    String entityType,
    Integer entityId,
    boolean publiclyVisible
) {
}
