package rs.teslaris.core.service.impl.commontypes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;

@Service
@RequiredArgsConstructor
public class TaskManagerServiceImpl implements TaskManagerService {

    private static final ConcurrentHashMap<String, ScheduledTask> tasks =
        new ConcurrentHashMap<>();

    private final ThreadPoolTaskScheduler taskScheduler;


    @Override
    public void scheduleTask(String taskId, LocalDateTime dateTime, Runnable task) {
        var executionTime = dateTime.atZone(ZoneId.systemDefault()).toInstant();

        Runnable wrappedTask = () -> {
            try {
                task.run();
            } finally {
                tasks.remove(taskId);
            }
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(wrappedTask, executionTime);
        tasks.put(taskId, new ScheduledTask(taskId, scheduledFuture, dateTime));
    }

    @Override
    public boolean cancelTask(String taskId) {
        ScheduledFuture<?> scheduledFuture = tasks.get(taskId).task();
        if (scheduledFuture != null) {
            boolean isCancelled =
                scheduledFuture.cancel(false); // Cancel the task without interrupting
            if (isCancelled) {
                tasks.remove(taskId);
            }
            return isCancelled;
        }
        return false; // Task not found
    }

    @Override
    public boolean isTaskScheduled(String taskId) {
        return tasks.containsKey(taskId);
    }

    @Override
    public List<ScheduledTaskResponseDTO> listScheduledTasks() {
        return tasks.values().stream().map(scheduledTask -> new ScheduledTaskResponseDTO(
            scheduledTask.id(), scheduledTask.executionTime)).collect(Collectors.toList());
    }

    private record ScheduledTask(
        String id,
        ScheduledFuture<?> task,
        LocalDateTime executionTime
    ) {
    }
}
