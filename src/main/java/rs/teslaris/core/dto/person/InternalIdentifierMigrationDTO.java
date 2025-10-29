package rs.teslaris.core.dto.person;

import java.time.LocalDate;
import java.util.Map;

public record PersonInternalIdentifierMigrationDTO(
    Map<Integer, Integer> oldToInternalIdMapping,
    Integer institutionId,
    LocalDate defaultInvolvementEndDate
) {
}
