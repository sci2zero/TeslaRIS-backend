package rs.teslaris.core.revision;

/**
 * Answers whether an identifier on an assessed record already belongs to another record of the
 * same kind, for entity types whose repository the data quality calculator may not depend on.
 * <p>
 * The calculator checks this itself wherever the entity lives in {@code core} - documents,
 * persons, organisation units and countries all do. For everything else the owning module
 * contributes an implementation, the same inversion {@link RevisionRestorer} and
 * {@link QualityScopeResolver} use.
 * <p>
 * Implementations must answer through the entity model rather than native SQL: {@code Project} and
 * {@code Funding} are soft-deleted behind {@code @SQLRestriction("deleted=false")}, which
 * Hibernate applies to JPQL and not to a native query, so a native check would report a deleted
 * record as a duplicate.
 */
public interface QualityUniquenessChecker {

    /**
     * The {@code EntityType} name this checker answers for, matching
     * {@link RevisionRestorer#entityType()}.
     */
    String entityType();

    /**
     * @param entityId the record being assessed, excluded from the search - a record is not its
     *                 own duplicate
     * @return whether another record already carries this value, {@code false} for an identifier
     * the entity does not have
     */
    boolean isDuplicate(QualityIdentifierField field, String value, Integer entityId);
}
