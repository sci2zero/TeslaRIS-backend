package rs.teslaris.core.dto.commontypes;

import java.time.Instant;

public record MaintenanceInformationDTO(
    Instant startTime,
    String approximateEndMoment
) {
}
