package rs.teslaris.core.dto.commontypes;

import java.time.LocalDateTime;
import rs.teslaris.core.model.commontypes.RecurrenceType;

public record ScheduledTaskResponseDTO(
    String taskId,
    LocalDateTime executionTime,
    RecurrenceType recurrenceType
) {
}
