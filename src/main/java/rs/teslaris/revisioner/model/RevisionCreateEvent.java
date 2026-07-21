package rs.teslaris.revisioner.model;

public record RevisionCreateEvent(
    String entityType,
    Integer entityId,
    Object oldObject,
    Object newObject,
    RevisionType revisionType
) {
}
