package rs.teslaris.core.service.impl.commontypes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.SchedulingException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskManagerServiceImpl implements TaskManagerService {

    private static final ConcurrentHashMap<String, ScheduledTask> tasks =
        new ConcurrentHashMap<>();

    private final ThreadPoolTaskScheduler taskScheduler;

    private final UserService userService;

    private final NotificationService notificationService;


    @Override
    public void scheduleTask(String taskId, LocalDateTime dateTime, Runnable task, Integer userId) {
        if (Objects.isNull(task)) {
            log.error("Trying to schedule null as task -> {}", taskId);
            return;
        }

        if (dateTime.isBefore(LocalDateTime.now())) {
            throw new SchedulingException("cantScheduleInPastMessage");
        }

        var executionTime = dateTime.atZone(ZoneId.systemDefault()).toInstant();

        Runnable wrappedTask = () -> {
            var notificationValues = new HashMap<String, String>();
            notificationValues.put("taskId", taskId);

            boolean taskSucceeded = false;
            long startTime = System.nanoTime();

            try {
                task.run();
                taskSucceeded = true;
            } catch (Exception e) {
                log.error("Task {} failed. Reason: {}", taskId, e.getMessage(), e);
            } finally {
                var duration = (System.nanoTime() - startTime) / 1000000000.0;
                notificationValues.put("duration", String.valueOf(duration));
                notificationService.createNotification(
                    NotificationFactory.contructScheduledTaskCompletedNotification(
                        notificationValues, userService.findOne(userId), taskSucceeded));
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

    @Override
    public List<ScheduledTaskResponseDTO> listScheduledReportGenerationTasks() {
        return tasks.values().stream().filter(task -> task.id.startsWith("ReportGeneration"))
            .map(scheduledTask -> new ScheduledTaskResponseDTO(
                scheduledTask.id(), scheduledTask.executionTime)).collect(Collectors.toList());
    }

    private record ScheduledTask(
        String id,
        ScheduledFuture<?> task,
        LocalDateTime executionTime
    ) {
    }
}
