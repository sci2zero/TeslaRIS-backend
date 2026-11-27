package rs.teslaris.revisioner.model.qualityassessment;

/**
 * Identifies a revised entity that has at least one data quality assessment, and serves as the
 * keyset cursor when walking assessments entity by entity.
 */
public record AssessedEntityKey(String entityType, Integer entityId) {

    /**
     * Sorts before every real key: entity types are non-empty enum names.
     */
    public static final AssessedEntityKey START = new AssessedEntityKey("", Integer.MIN_VALUE);
}
