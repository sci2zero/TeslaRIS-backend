package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import rs.teslaris.core.model.commontypes.RecurrenceType;
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
        var taskId = "task1";
        var executionTime = LocalDateTime.now().plusMinutes(10);
        Runnable task = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(
            invocation -> mock(ScheduledFuture.class));

        // When
        taskManagerService.scheduleTask(taskId, executionTime, task, 1, RecurrenceType.ONCE);

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

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 1, RecurrenceType.ONCE);

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

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2, RecurrenceType.ONCE);

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

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "INSTITUTIONAL_EDITOR"})
    public void shouldListScheduledHarvestTasksForUserOrAdmin(String role) {
        // Given
        var taskId1 = "Harvest-1-....";
        var taskId2 = "Harvest-2-....";
        var nonHarvestTaskId = "SomeOtherTaskType-999";

        var executionTime1 = LocalDateTime.now().plusDays(1);
        var executionTime2 = LocalDateTime.now().plusDays(2);

        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);
        var unrelatedTask = mock(Runnable.class);

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(nonHarvestTaskId, executionTime2, unrelatedTask, 2,
            RecurrenceType.ONCE);

        boolean isAdmin = role.equals("ADMIN");

        if (isAdmin) {
            when(userService.getUserOrganisationUnitId(1)).thenReturn(10);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(10))
                .thenReturn(List.of(10, 1, 2));
        } else {
            when(userService.getUserOrganisationUnitId(2)).thenReturn(5);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(5))
                .thenReturn(List.of(5, 6, 2));
        }

        // When
        var scheduledTasks = taskManagerService.listScheduledHarvestTasks(isAdmin ? 1 : 2, role);

        // Then
        if (isAdmin) {
            assertEquals(2, scheduledTasks.size());
            assertTrue(scheduledTasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .containsAll(List.of(taskId1, taskId2)));
        } else {
            assertEquals(1, scheduledTasks.size());
            assertEquals(taskId2, scheduledTasks.getFirst().taskId());
        }

        var executionTimes = scheduledTasks.stream()
            .map(ScheduledTaskResponseDTO::executionTime)
            .toList();

        if (isAdmin) {
            assertTrue(executionTimes.contains(executionTime2));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "INSTITUTIONAL_EDITOR"})
    void shouldListAllScheduledDocumentBackupGenerationTasks(String role) {
        // Given
        var taskId1 = "Document_Backup-1-...";
        var taskId2 = "Document_Backup-2-...";
        var executionTime1 = LocalDateTime.now().plusMinutes(15);
        var executionTime2 = LocalDateTime.now().plusHours(2);
        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2, RecurrenceType.ONCE);

        boolean isAdmin = role.equals("ADMIN");
        if (!isAdmin) {
            when(userService.getUserOrganisationUnitId(1)).thenReturn(1);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
                .thenReturn(List.of(1));
        }

        // When
        var tasks = taskManagerService.listScheduledDocumentBackupGenerationTasks(1, role);

        // Then
        if (isAdmin) {
            assertTrue(tasks.size() >= 2);
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .containsAll(List.of(taskId1, taskId2)));
        } else {
            assertTrue(!tasks.isEmpty());
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .contains(taskId1));
        }

        assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::executionTime).toList()
            .contains(executionTime1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "INSTITUTIONAL_LIBRARIAN", "HEAD_OF_LIBRARY"})
    void shouldListAllScheduledThesisLibraryBackupGenerationTasks(String role) {
        // Given
        var taskId1 = "Library_Backup-1-...";
        var taskId2 = "Library_Backup-2-...";
        var executionTime1 = LocalDateTime.now().plusMinutes(30);
        var executionTime2 = LocalDateTime.now().plusHours(3);
        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2, RecurrenceType.ONCE);

        boolean isAdmin = role.equals("ADMIN");
        if (!isAdmin) {
            when(userService.getUserOrganisationUnitId(1)).thenReturn(1);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
                .thenReturn(List.of(1));
        }

        // When
        var tasks = taskManagerService.listScheduledThesisLibraryBackupGenerationTasks(1, role);

        // Then
        if (isAdmin) {
            assertTrue(tasks.size() >= 2);
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .containsAll(List.of(taskId1, taskId2)));
        } else {
            assertFalse(tasks.isEmpty());
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .contains(taskId1));
        }

        assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::executionTime).toList()
            .contains(executionTime1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "PROMOTION_REGISTRY_ADMINISTRATOR"})
    void shouldListAllScheduledRegistryBookTasks(String role) {
        // Given
        var taskId1 = "Registry_Book-1-...";
        var taskId2 = "Registry_Book-2-...";
        var executionTime1 = LocalDateTime.now().plusMinutes(30);
        var executionTime2 = LocalDateTime.now().plusHours(3);
        var task1 = mock(Runnable.class);
        var task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
            .thenAnswer(invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1, RecurrenceType.ONCE);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 2, RecurrenceType.ONCE);

        boolean isAdmin = role.equals("ADMIN");
        if (!isAdmin) {
            when(userService.getUserOrganisationUnitId(1)).thenReturn(1);
            when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
                .thenReturn(List.of(1));
        }

        // When
        var tasks = taskManagerService.listScheduledRegistryBookGenerationTasks(1, role);

        // Then
        if (isAdmin) {
            assertTrue(tasks.size() >= 2);
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .containsAll(List.of(taskId1, taskId2)));
        } else {
            assertFalse(tasks.isEmpty());
            assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::taskId).toList()
                .contains(taskId1));
        }

        assertTrue(tasks.stream().map(ScheduledTaskResponseDTO::executionTime).toList()
            .contains(executionTime1));
    }
}
