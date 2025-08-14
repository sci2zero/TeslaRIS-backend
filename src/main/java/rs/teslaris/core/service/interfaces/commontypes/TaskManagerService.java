package rs.teslaris.core.service.interfaces.commontypes;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;

@Service
public interface TaskManagerService {

    String scheduleTask(String taskId, LocalDateTime dateTime, Runnable task, Integer userId,
                        RecurrenceType recurrence);

    boolean cancelTask(String taskId);

    boolean isTaskScheduled(String taskId);

    List<ScheduledTaskResponseDTO> listScheduledTasks();

    List<ScheduledTaskResponseDTO> listScheduledReportGenerationTasks(Integer userId, String role);

    List<ScheduledTaskResponseDTO> listScheduledHarvestTasks(Integer userId, String role);

    List<ScheduledTaskResponseDTO> listScheduledDocumentBackupGenerationTasks(Integer userId,
                                                                              String role);

    List<ScheduledTaskResponseDTO> listScheduledThesisLibraryBackupGenerationTasks(Integer userId,
                                                                                   String role);

    List<ScheduledTaskResponseDTO> listScheduledRegistryBookGenerationTasks(Integer userId,
                                                                            String role);

    LocalDateTime findNextFreeExecutionTime();

    void saveTaskMetadata(ScheduledTaskMetadata scheduledTask);
}
