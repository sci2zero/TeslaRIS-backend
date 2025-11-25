package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.commontypes.ScheduledTaskResponseDTO;
import rs.teslaris.core.model.commontypes.ApplicationConfiguration;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.repository.commontypes.ApplicationConfigurationRepository;
import rs.teslaris.core.repository.commontypes.ScheduledTaskMetadataRepository;
import rs.teslaris.core.service.impl.commontypes.ApplicationConfigurationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.jwt.JwtUtil;

@SpringBootTest
class ApplicationConfigurationServiceTest {

    @Mock
    private ApplicationConfigurationRepository applicationConfigurationRepository;

    @Mock
    private TaskManagerService taskManagerService;

    @Mock
    private ScheduledTaskMetadataRepository metadataRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private ApplicationConfigurationServiceImpl configurationService;


    @Test
    void shouldScheduleMaintenanceModeWithCorrectParameters() {
        // Given
        var startTime = LocalDateTime.now().plusHours(1);
        var approximateEndMoment = "2 hours";
        var userId = 123;
        var expectedTaskId = "task-123";

        when(taskManagerService.scheduleTask(anyString(), any(LocalDateTime.class),
            any(Runnable.class), eq(userId), eq(RecurrenceType.ONCE)))
            .thenReturn(expectedTaskId);

        // When
        configurationService.scheduleMaintenanceMode(startTime, approximateEndMoment, userId);

        // Then
        verify(taskManagerService).scheduleTask(
            startsWith("Maintenance-" + startTime),
            eq(startTime),
            any(Runnable.class),
            eq(userId),
            eq(RecurrenceType.ONCE)
        );

        verify(taskManagerService).saveTaskMetadata(argThat(metadata ->
            metadata.getTaskId().equals(expectedTaskId) &&
                metadata.getTimeToRun().equals(startTime) &&
                metadata.getType().equals(ScheduledTaskType.MAINTENANCE) &&
                metadata.getMetadata().get("approximateEndMoment").equals(approximateEndMoment) &&
                metadata.getMetadata().get("userId").equals(userId) &&
                metadata.getRecurrenceType().equals(RecurrenceType.ONCE)
        ));
    }

    @Test
    void shouldTurnOnMaintenanceModeAndRevokeTokens() {
        // Given
        var config = new ApplicationConfiguration();
        config.setIsInMaintenanceMode(false);

        when(applicationConfigurationRepository.findAll()).thenReturn(List.of(config));
        when(applicationConfigurationRepository.save(any(ApplicationConfiguration.class)))
            .thenReturn(config);

        // When
        configurationService.turnOnMaintenanceMode();

        // Then
        assertTrue(config.getIsInMaintenanceMode());
        verify(jwtUtil).revokeAllNonAdminTokens();
        verify(applicationConfigurationRepository).save(config);
    }

    @Test
    void shouldTurnOffMaintenanceMode() {
        // Given
        var config = new ApplicationConfiguration();
        config.setIsInMaintenanceMode(true);

        when(applicationConfigurationRepository.findAll()).thenReturn(List.of(config));
        when(applicationConfigurationRepository.save(any(ApplicationConfiguration.class)))
            .thenReturn(config);

        // When
        configurationService.turnOffMaintenanceMode();

        // Then
        assertFalse(config.getIsInMaintenanceMode());
        verify(applicationConfigurationRepository).save(config);
    }

    @Test
    void shouldReturnTrueWhenApplicationIsInMaintenanceMode() {
        // Given
        var config = new ApplicationConfiguration();
        config.setIsInMaintenanceMode(true);

        when(applicationConfigurationRepository.findAll()).thenReturn(List.of(config));

        // When
        boolean result = configurationService.isApplicationInMaintenanceMode();

        // Then
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenApplicationIsNotInMaintenanceMode() {
        // Given
        var config = new ApplicationConfiguration();
        config.setIsInMaintenanceMode(false);

        when(applicationConfigurationRepository.findAll()).thenReturn(List.of(config));

        // When
        boolean result = configurationService.isApplicationInMaintenanceMode();

        // Then
        assertFalse(result);
    }

    @Test
    void shouldReturnNextScheduledMaintenanceWhenTasksExist() {
        // Given
        var executionTime = LocalDateTime.now().plusHours(2);
        var approximateEndMoment = "3 hours";
        var taskId = "maintenance-task-123";

        var maintenanceTask =
            new ScheduledTaskResponseDTO(taskId, executionTime, RecurrenceType.ONCE);
        var taskMetadata = new ScheduledTaskMetadata(taskId, executionTime,
            ScheduledTaskType.MAINTENANCE, Map.of("approximateEndMoment", approximateEndMoment),
            RecurrenceType.ONCE);

        when(taskManagerService.listScheduledMaintenanceTasks())
            .thenReturn(List.of(maintenanceTask));
        when(metadataRepository.findTaskByTaskId(taskId))
            .thenReturn(Optional.of(taskMetadata));

        // When
        var result = configurationService.getNextScheduledMaintenance();

        // Then
        assertNotNull(result);
        assertEquals(executionTime, result.startTime());
        assertEquals(approximateEndMoment, result.approximateEndMoment());
    }

    @Test
    void shouldReturnNullWhenNoScheduledMaintenanceTasksExist() {
        // Given
        when(taskManagerService.listScheduledMaintenanceTasks()).thenReturn(List.of());

        // When
        var result = configurationService.getNextScheduledMaintenance();

        // Then
        assertNull(result);
        verify(metadataRepository, never()).findTaskByTaskId(anyString());
    }

    @Test
    void shouldReturnNullWhenTaskMetadataNotFound() {
        // Given
        var executionTime = LocalDateTime.now().plusHours(2);
        var taskId = "maintenance-task-123";
        var maintenanceTask =
            new ScheduledTaskResponseDTO(taskId, executionTime, RecurrenceType.ONCE);

        when(taskManagerService.listScheduledMaintenanceTasks())
            .thenReturn(List.of(maintenanceTask));
        when(metadataRepository.findTaskByTaskId(taskId))
            .thenReturn(Optional.empty());

        // When
        var result = configurationService.getNextScheduledMaintenance();

        // Then
        assertNull(result);
    }
}
