package rs.teslaris.migrator.util;

import lombok.Getter;

/**
 * The item matched an existing entity, which was supplemented instead of created. Thrown by the
 * creator so the runner records the item as RESOLVED, see {@code FailureHandler#resolvedToExisting}.
 */
@Getter
public class ResolvedToExistingException extends MigrationException {

    private final Integer entityId;


    public ResolvedToExistingException(Integer entityId, String message) {
        super(message);
        this.entityId = entityId;
    }
}
