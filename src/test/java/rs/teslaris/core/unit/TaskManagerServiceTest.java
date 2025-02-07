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
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import rs.teslaris.core.service.impl.commontypes.TaskManagerServiceImpl;

@SpringBootTest
class TaskManagerServiceTest {

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

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
        String taskId1 = "task1";
        String taskId2 = "task2";
        LocalDateTime executionTime1 = LocalDateTime.now().plusMinutes(10);
        LocalDateTime executionTime2 = LocalDateTime.now().plusHours(1);
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(
            invocation -> mock(ScheduledFuture.class));

        taskManagerService.scheduleTask(taskId1, executionTime1, task1, 1);
        taskManagerService.scheduleTask(taskId2, executionTime2, task2, 1);

        // When
        var scheduledTasks = taskManagerService.listScheduledTasks();

        // Then
        assertEquals(2, scheduledTasks.size());
        assertEquals(taskId1, scheduledTasks.get(0).taskId());
        assertEquals(executionTime1, scheduledTasks.get(0).executionTime());
        assertEquals(taskId2, scheduledTasks.get(1).taskId());
        assertEquals(executionTime2, scheduledTasks.get(1).executionTime());
    }
}
