package rs.teslaris.migrator.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.applicationevent.MigrationRunFinishedEvent;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.pipeline.MigrationPipeline;
import rs.teslaris.migrator.pipeline.MigrationPipelineRunner;
import rs.teslaris.migrator.pipeline.ResolvedPipeline;
import rs.teslaris.migrator.pipeline.RunScopedCache;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.service.impl.MigrationRunExecutor;
import rs.teslaris.migrator.util.MigrationEntityType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MigrationRunExecutorTest {

    @Mock
    private MigrationPipelineRunner pipelineRunner;

    @Mock
    private MigrationRunRepository runRepository;

    @Mock
    private MigrationRecordLogRepository recordLogRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RunScopedCache firstCache;

    @Mock
    private RunScopedCache secondCache;

    private MigrationRunExecutor executor;


    @BeforeEach
    public void setUp() {
        executor = new MigrationRunExecutor(pipelineRunner, runRepository,
            List.of(firstCache, secondCache), recordLogRepository, eventPublisher);
    }

    @Test
    public void shouldClearEveryRunScopedCacheBeforeTheRun() {
        // given
        var run = MigrationRun.builder()
            .id("run-1")
            .source("test-source")
            .entityType(MigrationEntityType.PERSON)
            .status(MigrationRunStatus.RUNNING)
            .startedAt(Instant.now())
            .batchSize(2)
            .currentPage(0)
            .build();
        var resolved = new ResolvedPipeline<>(mock(MigrationPipeline.class),
            MigrationEntityType.PERSON);

        // when
        executor.execute(resolved, run);

        // then
        var order = inOrder(firstCache, secondCache, pipelineRunner);
        order.verify(firstCache).clearCache();
        order.verify(secondCache).clearCache();
        order.verify(pipelineRunner).run(any(), any());
    }

    @Test
    public void shouldAnnounceTheUpdatedEntitiesOfAnUpdateRun() {
        // given
        var run = MigrationRun.builder()
            .id("run-3")
            .source("test-source")
            .entityType(MigrationEntityType.PERSON)
            .status(MigrationRunStatus.RUNNING)
            .startedAt(Instant.now())
            .batchSize(2)
            .currentPage(0)
            .isUpdate(true)
            .build();
        var resolved = new ResolvedPipeline<>(mock(MigrationPipeline.class),
            MigrationEntityType.PERSON);

        when(recordLogRepository.findCreatedEntityTypes("run-3")).thenReturn(Set.of("PERSON"));
        when(recordLogRepository.findTargetIdsByType("run-3", MigrationItemStatus.UPDATED))
            .thenReturn(Map.of("PERSON", List.of(5)));

        // when
        executor.execute(resolved, run);

        // then
        var event = ArgumentCaptor.forClass(MigrationRunFinishedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertEquals(Map.of("PERSON", List.of(5)), event.getValue().updatedEntityIdsByType());
        assertNull(event.getValue().createdEntityIdsByType());
        verify(recordLogRepository, never())
            .findTargetIdsByType("run-3", MigrationItemStatus.CREATED);
    }

    @Test
    public void shouldRunTheSamePipelineWhenCalledSynchronously() {
        // given
        var run = MigrationRun.builder()
            .id("run-2")
            .source("test-source")
            .entityType(MigrationEntityType.PERSON)
            .status(MigrationRunStatus.RUNNING)
            .startedAt(Instant.now())
            .batchSize(2)
            .currentPage(0)
            .build();
        var resolved = new ResolvedPipeline<>(mock(MigrationPipeline.class),
            MigrationEntityType.PERSON);

        // when
        executor.executeSynchronous(resolved, run);

        // then
        verify(pipelineRunner).run(any(), any());
    }
}
