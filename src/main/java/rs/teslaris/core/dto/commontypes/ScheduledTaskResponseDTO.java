package rs.teslaris.core.dto.commontypes;

import java.time.Instant;
import rs.teslaris.core.model.commontypes.RecurrenceType;

public record ScheduledTaskResponseDTO(
    String taskId,
    Instant executionTime,
    RecurrenceType recurrenceType
) {
}
