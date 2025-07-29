package rs.teslaris.core.service.impl.commontypes;

import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
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
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Traceable
public class TaskManagerServiceImpl implements TaskManagerService {

    private static final ConcurrentHashMap<String, ScheduledTask> tasks =
        new ConcurrentHashMap<>();

    private static final Duration STEP = Duration.ofMinutes(1);

    private final ThreadPoolTaskScheduler taskScheduler;

    private final UserService userService;

    private final OrganisationUnitService organisationUnitService;

    private final NotificationService notificationService;

    private final ScheduledTaskMetadataRepository scheduledTaskMetadataRepository;


    @Override
    @Nullable
    public String scheduleTask(String taskId, LocalDateTime dateTime, Runnable task,
                               Integer userId, RecurrenceType recurrence) {
        if (Objects.isNull(task)) {
            log.error("Trying to schedule null as task -> {}", taskId);
            return null;
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
                double duration = (System.nanoTime() - startTime) / 1_000_000_000.0;
                notificationValues.put("duration", String.valueOf(duration));

                var user = userService.findOne(userId);
                if (taskId.startsWith("Registry_Book")) {
                    notificationService.createNotification(
                        NotificationFactory.contructScheduledReportGenerationCompletedNotification(
                            notificationValues, user, taskSucceeded));
                } else if (taskId.contains("Backup")) {
                    notificationService.createNotification(
                        NotificationFactory.contructScheduledBackupGenerationCompletedNotification(
                            notificationValues, user, taskSucceeded));
                } else {
                    notificationService.createNotification(
                        NotificationFactory.contructScheduledTaskCompletedNotification(
                            notificationValues, user, taskSucceeded));
                }

                // Clean up from in-memory structure
                tasks.remove(taskId);

                if (Objects.nonNull(recurrence) && !recurrence.equals(RecurrenceType.ONCE)) {
                    // Reschedule if needed
                    LocalDateTime nextExecutionTime = switch (recurrence) {
                        case DAILY -> dateTime.plusDays(1);
                        case WEEKLY -> dateTime.plusWeeks(1);
                        case MONTHLY -> dateTime.plusMonths(1);
                        case THREE_MONTHLY -> dateTime.plusMonths(3);
                        case YEARLY -> dateTime.plusYears(1);
                        default -> null;
                    };

                    if (Objects.nonNull(nextExecutionTime)) {
                        log.info("Rescheduling task {} for {}", taskId, nextExecutionTime);
                        scheduleTask(taskId, nextExecutionTime, task, userId, recurrence);
                    }
                } else {
                    // Remove from DB if reached end-of-life
                    scheduledTaskMetadataRepository.deleteTaskForTaskId(taskId);
                }
            }
        };

        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(wrappedTask, executionTime);
        tasks.put(taskId, new ScheduledTask(taskId, scheduledFuture, dateTime));

        return taskId;
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
    public List<ScheduledTaskResponseDTO> listScheduledReportGenerationTasks(Integer userId,
                                                                             String role) {
        boolean isAdmin = UserRole.ADMIN.name().equals(role);

        List<Integer> subOUs = isAdmin
            ? Collections.emptyList()
            : getUserSubOrganisationUnits(userId);

        return tasks.values().stream()
            .filter(task -> isReportGenerationTask(task.id) &&
                (isAdmin || isTaskInSubOU(task.id, subOUs)))
            .map(task -> new ScheduledTaskResponseDTO(task.id(), task.executionTime))
            .collect(Collectors.toList());
    }

    @Override
    public LocalDateTime findNextFreeExecutionTime() {
        var now = LocalDateTime.now().withSecond(0).withNano(0);
        var candidate = now.plus(STEP);

        var scheduledTimes = tasks.values().stream()
            .map(ScheduledTask::executionTime)
            .collect(Collectors.toSet()); // Using Set for O(1) lookup

        while (scheduledTimes.contains(candidate)) {
            candidate = candidate.plus(STEP);
        }

        return candidate;
    }

    @Override
    public void saveTaskMetadata(ScheduledTaskMetadata scheduledTask) {
        scheduledTaskMetadataRepository.save(scheduledTask);
    }

    private boolean isReportGenerationTask(String taskId) {
        return taskId.startsWith("ReportGeneration-");
    }

    private boolean isTaskInSubOU(String taskId, List<Integer> subOUs) {
        try {
            int ouId = Integer.parseInt(taskId.split("-")[1]);
            return subOUs.contains(ouId);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return false;
        }
    }

    private List<Integer> getUserSubOrganisationUnits(Integer userId) {
        int employmentInstitutionId = userService.getUserOrganisationUnitId(userId);
        return new ArrayList<>(
            organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(
                employmentInstitutionId));
    }

    private record ScheduledTask(
        String id,
        ScheduledFuture<?> task,
        LocalDateTime executionTime
    ) {
    }
}
