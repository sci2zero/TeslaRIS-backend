package rs.teslaris.core.dto.commontypes;

import java.time.LocalDateTime;

public record ScheduledTaskResponseDTO(
    String taskId,
    LocalDateTime executionTime
) {
}
