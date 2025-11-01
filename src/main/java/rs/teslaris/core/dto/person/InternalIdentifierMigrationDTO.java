package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import java.util.Map;

public record InternalIdentifierMigrationDTO(
    Map<Integer, Integer> oldToInternalIdMapping,
    Integer institutionId,
    LocalDate defaultInvolvementEndDate,
    boolean accountingIds
) {
}
