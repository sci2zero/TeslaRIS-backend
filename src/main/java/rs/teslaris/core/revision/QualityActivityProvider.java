package rs.teslaris.core.revision;

import java.util.List;
import rs.teslaris.core.dto.document.OrganisationUnitContributionDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;

/**
 * Gives the data quality calculator the contributions of a record whose repository it must not
 * depend on, which is the same inversion {@link RevisionRestorer} and
 * {@link QualityUniquenessChecker} use.
 * <p>
 * The contributions are read when the assessment runs. An assessment is computed once, and the
 * backfill reassesses a revision only when the record did not change after it, so the snapshot and
 * the contributions describe the same moment.
 */
public interface QualityActivityProvider {

    /**
     * The {@code EntityType} name this provider answers for, matching
     * {@link RevisionRestorer#entityType()}.
     */
    String entityType();

    List<PersonContributionDTO> personContributions(Integer entityId);

    List<OrganisationUnitContributionDTO> organisationUnitContributions(Integer entityId);
}
