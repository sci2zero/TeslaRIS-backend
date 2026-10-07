package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.project.dto.funding.FundingDTO;

/**
 * Project, funder and currency are resolved at creation time; a non-null {@code rejection} fails
 * the item.
 */
public record FundingMigrationDTO(
    String projectSourceKey,
    String sourceId,
    HydratorCVModel.Institution funder,
    FundingDTO funding,
    String rejection
) {
}
