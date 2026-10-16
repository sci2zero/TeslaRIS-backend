package rs.teslaris.core.migration;

import jakarta.annotation.Nullable;
import rs.teslaris.core.dto.funding.FundingDTO;

/**
 * Writes a migrated funding, so that a module importing from an external source does not have to
 * depend on the project module. Implemented there, consumed by the importing module.
 */
public interface FundingMigrationWriter {

    /**
     * Idempotent in the migrated record, on the same terms as
     * {@link ProjectMigrationWriter#migrate}.
     *
     * @param funding           the funding to write
     * @param existingFundingId id a previous run produced for the same source record, or
     *                          {@code null} when this record has not been migrated yet
     */
    MigratedFunding migrate(FundingDTO funding, @Nullable Integer existingFundingId);

    /**
     * @param id      the written funding
     * @param created false when an existing funding was updated instead
     */
    record MigratedFunding(Integer id, boolean created) {
    }
}
