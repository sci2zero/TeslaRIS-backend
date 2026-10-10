package rs.teslaris.core.revision;

import java.util.Optional;

/**
 * Supplies the owning persons, owning institutions and display name of an assessed record whose
 * module the data quality indexer may not depend on.
 * <p>
 * The indexer resolves those itself for the entity types whose indexes live in {@code core}. For
 * everything else the owning module contributes an implementation, the same inversion
 * {@link RevisionRestorer} uses, and the indexer picks it up as a {@code List} of beans keyed by
 * {@link #entityType()}.
 * <p>
 * Implementations are called once per indexed assessment, so they should answer from a single
 * index lookup rather than a chain of them.
 */
public interface QualityScopeResolver {

    /**
     * The {@code EntityType} name this resolver answers for, matching
     * {@link RevisionRestorer#entityType()}.
     */
    String entityType();

    /**
     * @return the record's scope, or empty when the record is not indexed yet - the assessment is
     * then written with no scope rather than not written at all
     */
    Optional<QualityScope> resolve(Integer entityId);
}
