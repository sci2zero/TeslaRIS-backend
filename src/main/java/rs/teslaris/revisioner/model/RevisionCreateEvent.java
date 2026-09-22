package rs.teslaris.revisioner.model;

import rs.teslaris.core.util.restoration.RestorationContext;

public record RevisionCreateEvent(
    String entityType,
    Integer entityId,
    Object oldObject,
    Object newObject,
    RevisionType revisionType,
    boolean duringRestoration
) {

    public RevisionCreateEvent(String entityType, Integer entityId, Object oldObject,
                               Object newObject, RevisionType revisionType) {
        this(entityType, entityId, oldObject, newObject, revisionType,
            RestorationContext.isActive());
    }
}
