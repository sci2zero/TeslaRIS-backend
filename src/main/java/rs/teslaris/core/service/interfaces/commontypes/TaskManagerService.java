package rs.teslaris.core.service.interfaces.commontypes;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;

@Service
public interface TaskManagerService {

    void scheduleTask(String taskId, LocalDateTime dateTime, Runnable task);

    boolean cancelTask(String taskId);

    boolean isTaskScheduled(String taskId);

    List<ScheduledTaskResponseDTO> listScheduledTasks();
}
