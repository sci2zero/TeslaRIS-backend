package rs.teslaris.core.dto.commontypes;

import java.time.LocalDateTime;

public record MaintenanceInformationDTO(
    LocalDateTime startTime,
    String approximateEndMoment
) {
}
