package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.migrator.configuration.MigrationSourceProperties;
import rs.teslaris.migrator.dto.MigrationScheduleRequestDTO;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.MigrationPipeline;
import rs.teslaris.migrator.pipeline.MigrationPipelineRegistry;
import rs.teslaris.migrator.pipeline.ResolvedPipeline;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.service.impl.MigrationRunExecutor;
import rs.teslaris.migrator.service.impl.MigrationServiceImpl;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MigrationSchedulingTest {

    private static final Instant TIME_TO_RUN = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private MigrationPipelineRegistry pipelineRegistry;

    @Mock
    private MigrationRunExecutor runExecutor;

    @Mock
    private MigrationRunRepository runRepository;

    @Mock
    private MigrationRecordLogRepository recordLogRepository;

    @Mock
    private TaskManagerService taskManagerService;

    private MigrationServiceImpl service;

    private static MigrationScheduleRequestDTO request(Integer modifiedAfterDays) {
        return new MigrationScheduleRequestDTO("test-source", MigrationEntityType.PERSON, 10, true,
            modifiedAfterDays, false, true, 5);
    }

    @BeforeEach
    public void setUp() {
        doReturn(Optional.of(new ResolvedPipeline<>(mock(MigrationPipeline.class),
            MigrationEntityType.PERSON))).when(pipelineRegistry).resolve(anyString(), any());
        when(runRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskManagerService.scheduleTask(anyString(), any(), any(), any(), any()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service = new MigrationServiceImpl(pipelineRegistry, runExecutor, runRepository,
            recordLogRepository, taskManagerService, new MigrationSourceProperties());
    }

    @Test
    public void shouldScheduleOneTaskPerEntityTypeAndKeepItsParameters() {
        // when
        service.scheduleRun(TIME_TO_RUN, request(3), RecurrenceType.DAILY);

        // then
        var taskId = ArgumentCaptor.forClass(String.class);
        verify(taskManagerService).scheduleTask(taskId.capture(), eq(TIME_TO_RUN), any(), eq(5),
            eq(RecurrenceType.DAILY));
        assertTrue(taskId.getValue().startsWith("Migration_Sync-test-source-PERSON-"));

        var metadata = ArgumentCaptor.forClass(ScheduledTaskMetadata.class);
        verify(taskManagerService).saveTaskMetadata(metadata.capture());
        assertEquals(ScheduledTaskType.MIGRATION_SYNC, metadata.getValue().getType());
        assertEquals("test-source", metadata.getValue().getMetadata().get("source"));
        assertEquals("PERSON", metadata.getValue().getMetadata().get("entityType"));
        assertEquals(3, metadata.getValue().getMetadata().get("modifiedAfterDays"));
        assertEquals(true, metadata.getValue().getMetadata().get("isUpdate"));
        assertEquals(5, metadata.getValue().getMetadata().get("userId"));
    }

    @Test
    public void shouldComputeTheWindowOfEveryFiringRelativeToIt() {
        // given
        service.scheduleRun(TIME_TO_RUN, request(3), RecurrenceType.DAILY);

        var task = ArgumentCaptor.forClass(Runnable.class);
        verify(taskManagerService).scheduleTask(anyString(), any(), task.capture(), any(), any());

        // when
        task.getValue().run();

        // then
        var run = ArgumentCaptor.forClass(MigrationRun.class);
        verify(runRepository).save(run.capture());
        assertFalse(run.getValue().getModifiedAfter().isBlank());
        assertTrue(Duration.between(Instant.parse(run.getValue().getModifiedAfter()),
            Instant.now()).toHours() >= 71);
        assertTrue(run.getValue().isUpdate());
        verify(runExecutor).executeSynchronous(any(), any());
        verify(runExecutor, never()).execute(any(), any());
    }

    @Test
    public void shouldRejectAWindowShorterThanOneDay() {
        assertThrows(MigrationException.class,
            () -> service.scheduleRun(TIME_TO_RUN, request(0), RecurrenceType.DAILY));
        verify(taskManagerService, never()).scheduleTask(anyString(), any(), any(), any(), any());
    }
}
