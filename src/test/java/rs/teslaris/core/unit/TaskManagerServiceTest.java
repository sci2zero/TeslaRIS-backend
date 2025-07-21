package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.service.impl.commontypes.TaskManagerServiceImpl;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
class TaskManagerServiceTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private UserService userService;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @InjectMocks
    private TaskManagerServiceImpl taskManagerService;


    @Test
    public void shouldScheduleTaskSuccessfully() {
        // Given
        String taskId = "task1";
        var executionTime = LocalDateTime.now().plusMinutes(10);
        Runnable task = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(
            invocation -> mock(ScheduledFuture.class));

        // When
        taskManagerService.scheduleTask(taskId, executionTime, task, 1);

        // Then
        assertTrue(taskManagerService.isTaskScheduled(taskId));
        verify(taskScheduler, times(1)).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    public void shouldListAllScheduledTasks() {
        // Given
        var taskId1 = "task1";
        var taskId2 = "task2";
        var executionTime1 = LocalDateTime.now().plusMinutes(10);
        var executionTime2 = LocalDateTime.now().plusHours(1);
        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(
            invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 1);

        // When
        var scheduledTasks = taskManagerService.listScheduledTasks();

        // Then
        assertTrue(scheduledTasks.size() >= 2);
        assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
            .containsAll(List.of(taskId1, taskId2)));
        assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::executionTime).toList()
            .containsAll(List.of(executionTime1, executionTime2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "VICE_DEAN_FOR_SCIENCE"})
    public void shouldListAllScheduledReportGenerationTasks(String role) {
        // Given
        var taskId1 = "ReportGeneration-1-.....";
        var taskId2 = "ReportGeneration-2-.....";
        var executionTime1 = LocalDateTime.now().plusMinutes(10);
        var executionTime2 = LocalDateTime.now().plusHours(1);
        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2);

        // Mock behavior for non-admin roles
        boolean isAdmin = role.equals("ADMIN");
        if (!isAdmin) {
            when(userService.getUserOrganisationUnitId(1)).thenReturn(1);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
                .thenReturn(List.of(1)); // Assuming sub-OU structure
        }

        // When
        var scheduledTasks = taskManagerService.listScheduledReportGenerationTasks(1, role);

        // Then
        if (isAdmin) {
            assertEquals(2, scheduledTasks.size());
            assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .containsAll(List.of(taskId1, taskId2)));
        } else {
            assertEquals(1, scheduledTasks.size());
            assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .contains(taskId1));
        }

        assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::executionTime).toList()
            .contains(executionTime1));
    }
}
