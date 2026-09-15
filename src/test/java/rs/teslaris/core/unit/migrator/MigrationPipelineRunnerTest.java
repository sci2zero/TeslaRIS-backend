package rs.teslaris.core.unit.migrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.migrator.client.MigrationCursor;
import rs.teslaris.migrator.client.SourceBatch;
import rs.teslaris.migrator.client.SourceRecordFetcher;
import rs.teslaris.migrator.model.MigrationItemStatus;
import rs.teslaris.migrator.model.MigrationRecordLog;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.model.MigrationRunStatus;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.FailureResolution;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationItem;
import rs.teslaris.migrator.pipeline.MigrationPipeline;
import rs.teslaris.migrator.pipeline.MigrationPipelineRunner;
import rs.teslaris.migrator.pipeline.ResolvedPipeline;
import rs.teslaris.migrator.pipeline.RetryPolicy;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.repository.MigrationRunRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MigrationPipelineRunnerTest {

    private static final String SOURCE = "test-source";

    @Mock
    private MigrationRunRepository runRepository;

    @Mock
    private MigrationRecordLogRepository recordLogRepository;

    @Mock
    private MigrationLog migrationLog;

    @InjectMocks
    private MigrationPipelineRunner runner;


    private MigrationRun newRun(MigrationEntityType entityType) {
        return MigrationRun.builder()
            .id("run-1")
            .source(SOURCE)
            .entityType(entityType)
            .status(MigrationRunStatus.RUNNING)
            .startedAt(Instant.now())
            .batchSize(2)
            .currentPage(0)
            .build();
    }

    private MigrationPipeline<String> pipeline(SourceRecordFetcher<String> fetcher,
                                               ItemRouter<String> router) {
        return new MigrationPipeline<>(SOURCE, MigrationEntityType.PERSON, String.class, fetcher,
            router, RetryPolicy.none(), Function.identity(), 2);
    }

    private ItemRouter<String> singleItemRouter(EntityCreator<String> creator,
                                                FailureHandler<String> failureHandler) {
        return record -> List.of(new MigrationItem<>(
            MigrationEntityType.PERSON, record, record, creator, failureHandler));
    }

    private SourceRecordFetcher<String> pagedFetcher(List<List<String>> pages) {
        return (cursor, batchSize) -> {
            if (cursor.page() >= pages.size()) {
                return SourceBatch.empty(cursor.nextPage());
            }

            return new SourceBatch<>(pages.get(cursor.page()), cursor.nextPage(),
                cursor.page() < pages.size() - 1);
        };
    }

    @Test
    public void shouldCreateEveryItemAcrossPages() {
        // given
        var created = new ArrayList<String>();
        var run = newRun(MigrationEntityType.PERSON);

        var resolved = new ResolvedPipeline<>(pipeline(
            pagedFetcher(List.of(List.of("a", "b"), List.of("c"))),
            singleItemRouter((dto, index) -> {
                created.add(dto);
                return created.size();
            }, FailureHandler.noOp())), MigrationEntityType.PERSON);

        // when
        var result = runner.run(resolved, run);

        // then
        assertEquals(List.of("a", "b", "c"), created);
        assertEquals(3, result.getItemsCreated());
        assertEquals(3, result.getRecordsRead());
        assertEquals(MigrationRunStatus.FINISHED, result.getStatus());
    }

    @Test
    public void shouldContinueWhenSingleItemFails() {
        // given
        var run = newRun(MigrationEntityType.PERSON);

        var resolved = new ResolvedPipeline<>(pipeline(
            pagedFetcher(List.of(List.of("ok-1", "boom", "ok-2"))),
            singleItemRouter((dto, index) -> {
                if ("boom".equals(dto)) {
                    throw new IllegalStateException("creation failed");
                }
                return 1;
            }, FailureHandler.noOp())), MigrationEntityType.PERSON);

        // when
        var result = runner.run(resolved, run);

        // then
        assertEquals(2, result.getItemsCreated());
        assertEquals(1, result.getItemsFailed());
        assertEquals(MigrationRunStatus.FINISHED, result.getStatus());

        verify(migrationLog).itemFailed(eq(run), any(), any());
    }

    @Test
    public void shouldContinueToNextPageWhenBatchFetchFails() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var created = new ArrayList<String>();

        SourceRecordFetcher<String> fetcher = (cursor, batchSize) -> switch (cursor.page()) {
            case 0 -> throw new IllegalStateException("source unreachable");
            case 1 -> new SourceBatch<>(List.of("recovered"), cursor.nextPage(), false);
            default -> SourceBatch.empty(cursor.nextPage());
        };

        var resolved = new ResolvedPipeline<>(pipeline(fetcher, singleItemRouter((dto, index) -> {
            created.add(dto);
            return 1;
        }, FailureHandler.noOp())), MigrationEntityType.PERSON);

        // when
        var result = runner.run(resolved, run);

        // then
        assertEquals(List.of("recovered"), created);
        assertEquals(1, result.getBatchesFailed());
        assertEquals(MigrationRunStatus.FINISHED, result.getStatus());

        verify(migrationLog).batchFailed(eq(run), any(MigrationCursor.class), any());
    }

    @Test
    public void shouldSkipItemsThatWereAlreadyProcessed() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var created = new ArrayList<String>();

        when(recordLogRepository.isAlreadyProcessed(SOURCE, MigrationEntityType.PERSON, "done"))
            .thenReturn(true);

        var resolved = new ResolvedPipeline<>(pipeline(
            pagedFetcher(List.of(List.of("done", "fresh"))),
            singleItemRouter((dto, index) -> {
                created.add(dto);
                return 1;
            }, FailureHandler.noOp())), MigrationEntityType.PERSON);

        // when
        var result = runner.run(resolved, run);

        // then
        assertEquals(List.of("fresh"), created);
        assertEquals(1, result.getItemsSkipped());
        assertEquals(1, result.getItemsCreated());
    }

    @Test
    public void shouldRetryUntilPolicyIsExhausted() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var attempts = new AtomicInteger();

        var pipeline = new MigrationPipeline<>(SOURCE, MigrationEntityType.PERSON, String.class,
            pagedFetcher(List.of(List.of("retry-me"))),
            singleItemRouter((dto, index) -> {
                attempts.incrementAndGet();
                throw new IllegalStateException("transient");
            }, (item, exception, attempt) -> FailureResolution.retry()),
            new RetryPolicy(3, java.time.Duration.ZERO, 1.0), Function.identity(), 2);

        // when
        var result = runner.run(new ResolvedPipeline<>(pipeline, MigrationEntityType.PERSON), run);

        // then
        assertEquals(3, attempts.get());
        assertEquals(1, result.getItemsFailed());
    }

    @Test
    public void shouldCountHandlerResolvedItemsAsResolvedInsteadOfFailed() {
        // given
        var run = newRun(MigrationEntityType.PERSON);

        var pipeline = pipeline(
            pagedFetcher(List.of(List.of("duplicate"))),
            singleItemRouter((dto, index) -> {
                throw new IllegalStateException("orcidIdExistsError");
            }, (item, exception, attempt) -> FailureResolution.resolved(42)));

        // when
        var result = runner.run(new ResolvedPipeline<>(pipeline, MigrationEntityType.PERSON), run);

        // then
        assertEquals(1, result.getItemsResolved());
        assertEquals(0, result.getItemsFailed());

        var captor = ArgumentCaptor.forClass(MigrationRecordLog.class);
        verify(recordLogRepository).record(captor.capture());

        assertEquals(MigrationItemStatus.RESOLVED, captor.getValue().getStatus());
        assertEquals(42, captor.getValue().getTargetEntityId());
    }

    @Test
    public void shouldOnlyProcessRequestedTypeWhenFallingBackToParentPipeline() {
        // given
        var run = newRun(MigrationEntityType.THESIS);
        var created = new ArrayList<String>();

        EntityCreator<String> creator = (dto, index) -> {
            created.add(dto);
            return 1;
        };

        ItemRouter<String> mixedRouter = record -> List.of(
            new MigrationItem<>(MigrationEntityType.JOURNAL_PUBLICATION, record + "-journal",
                record, creator, FailureHandler.noOp()),
            new MigrationItem<>(MigrationEntityType.THESIS, record + "-thesis", record, creator,
                FailureHandler.noOp()));

        var documentPipeline = new MigrationPipeline<>(SOURCE, MigrationEntityType.DOCUMENT,
            String.class, pagedFetcher(List.of(List.of("cv"))), mixedRouter, RetryPolicy.none(),
            Function.identity(), 2);

        // when
        var result = runner.run(
            new ResolvedPipeline<>(documentPipeline, MigrationEntityType.THESIS), run);

        // then
        assertEquals(1, created.size());
        assertEquals(1, result.getItemsCreated());

        verify(recordLogRepository, times(1)).record(any());
        verify(recordLogRepository, never())
            .isAlreadyProcessed(SOURCE, MigrationEntityType.JOURNAL_PUBLICATION, "cv-journal");
    }
}
