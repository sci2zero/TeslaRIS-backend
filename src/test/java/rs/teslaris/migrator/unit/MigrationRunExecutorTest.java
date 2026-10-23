package rs.teslaris.migrator.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
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
}
