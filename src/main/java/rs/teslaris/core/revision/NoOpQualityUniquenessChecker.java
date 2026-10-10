package rs.teslaris.core.revision;

import org.springframework.stereotype.Component;

/**
 * Keeps {@code List<QualityUniquenessChecker>} satisfiable in a deployment where no module
 * contributes a checker, so the data quality calculator still starts.
 * <p>
 * Note what this trades away: without a real checker the uniqueness rules of the affected entity
 * type are still declared in the profile but never report, so they read as always passing rather
 * than as unevaluated. That is the cost of not failing at startup.
 */
@Component
public class NoOpQualityUniquenessChecker implements QualityUniquenessChecker {

    @Override
    public String entityType() {
        return UnmatchedEntityType.VALUE;
    }

    @Override
    public boolean isDuplicate(QualityIdentifierField field, String value, Integer entityId) {
        return false;
    }
}
