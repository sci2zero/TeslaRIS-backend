package rs.teslaris.core.dto.commontypes;

import java.time.LocalDateTime;

public record MaintenanceModeDTO(
    LocalDateTime startTime,
    String approximateEndMoment
) {
}
