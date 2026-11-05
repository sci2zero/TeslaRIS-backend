package rs.teslaris.migrator.unit;

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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
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
import rs.teslaris.migrator.pipeline.UpdatePlan;
import rs.teslaris.migrator.pipeline.merge.MigrationMergePolicy;
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
    private MigrationMergePolicy mergePolicy;

    @Mock
    private MigrationLog migrationLog;

    @InjectMocks
    private MigrationPipelineRunner runner;


    @BeforeEach
    public void setUpLimits() {
        // The limits come from the properties, which no mock injects.
        ReflectionTestUtils.setField(runner, "batchFailureLimit", 25);
        ReflectionTestUtils.setField(runner, "consecutiveBatchFailureLimit", 3);
    }

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
        return (cursor, batchSize, run) -> {
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
            singleItemRouter((dto, migrationRun) -> {
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
            singleItemRouter((dto, migrationRun) -> {
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

        SourceRecordFetcher<String> fetcher =
            (cursor, batchSize, migrationRun) -> switch (cursor.page()) {
                case 0 -> throw new IllegalStateException("source unreachable");
                case 1 -> new SourceBatch<>(List.of("recovered"), cursor.nextPage(), false);
                default -> SourceBatch.empty(cursor.nextPage());
            };

        var resolved =
            new ResolvedPipeline<>(pipeline(fetcher, singleItemRouter((dto, migrationRun) -> {
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
    public void shouldFailRunWhenSourceKeepsFailing() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var attempts = new AtomicInteger();

        SourceRecordFetcher<String> fetcher = (cursor, batchSize, migrationRun) -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("source unreachable");
        };

        var resolved = new ResolvedPipeline<>(
            pipeline(fetcher, singleItemRouter((dto, migrationRun) -> 1, FailureHandler.noOp())),
            MigrationEntityType.PERSON);

        // when
        var result = runner.run(resolved, run);

        // then
        assertEquals(MigrationRunStatus.FAILED, result.getStatus());
        assertEquals(3, result.getBatchesFailed());

        // Three pages, and the pipeline of the fixture gives one attempt to each of them.
        assertEquals(3, attempts.get());

        verify(migrationLog).runAbandoned(eq(run), any(String.class));
    }

    @Test
    public void shouldSkipItemsThatWereAlreadyProcessed() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var created = new ArrayList<String>();

        when(recordLogRepository.find(SOURCE, MigrationEntityType.PERSON, "done"))
            .thenReturn(Optional.of(MigrationRecordLog.builder()
                .status(MigrationItemStatus.CREATED)
                .targetEntityId(7)
                .build()));

        var resolved = new ResolvedPipeline<>(pipeline(
            pagedFetcher(List.of(List.of("done", "fresh"))),
            singleItemRouter((dto, migrationRun) -> {
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
    public void shouldUpdateAlreadyWrittenItemWhenRunIsAnUpdate() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        run.setUpdate(true);

        var updated = new ArrayList<String>();

        when(recordLogRepository.find(SOURCE, MigrationEntityType.PERSON, "changed"))
            .thenReturn(Optional.of(MigrationRecordLog.builder()
                .status(MigrationItemStatus.CREATED)
                .targetEntityId(11)
                .build()));

        when(mergePolicy.merge(any(), any(), any())).thenAnswer(
            invocation -> invocation.getArgument(0));

        ItemRouter<String> router = record -> List.of(new MigrationItem<>(
            MigrationEntityType.PERSON, record, null, record,
            (dto, migrationRun) -> 1,
            UpdatePlan.of(targetId -> "stored",
                (targetId, dto, migrationRun) -> updated.add(targetId + ":" + dto)),
            FailureHandler.noOp()));

        // when
        var result = runner.run(new ResolvedPipeline<>(
            pipeline(pagedFetcher(List.of(List.of("changed"))), router),
            MigrationEntityType.PERSON), run);

        // then
        assertEquals(List.of("11:changed"), updated);
        assertEquals(1, result.getItemsUpdated());
        assertEquals(0, result.getItemsCreated());
        assertEquals(0, result.getItemsSkipped());

        var captor = ArgumentCaptor.forClass(MigrationRecordLog.class);
        verify(recordLogRepository).record(captor.capture());

        assertEquals(MigrationItemStatus.UPDATED, captor.getValue().getStatus());
    }

    @Test
    public void shouldAdoptTheEntityOfTheLegacyKeyInsteadOfCreatingASecondOne() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var created = new ArrayList<String>();

        when(recordLogRepository.find(SOURCE, MigrationEntityType.PERSON, "ror:123"))
            .thenReturn(Optional.empty());
        when(recordLogRepository.find(SOURCE, MigrationEntityType.PERSON, "old-name"))
            .thenReturn(Optional.of(MigrationRecordLog.builder()
                .status(MigrationItemStatus.CREATED)
                .targetEntityId(5)
                .build()));

        ItemRouter<String> router = record -> List.of(new MigrationItem<>(
            MigrationEntityType.PERSON, "ror:123", "old-name", record,
            (dto, migrationRun) -> {
                created.add(dto);
                return 1;
            },
            UpdatePlan.none(),
            FailureHandler.noOp()));

        // when
        var result = runner.run(new ResolvedPipeline<>(
            pipeline(pagedFetcher(List.of(List.of("curriculum"))), router),
            MigrationEntityType.PERSON), run);

        // then
        assertEquals(List.of(), created);
        assertEquals(1, result.getItemsSkipped());

        var captor = ArgumentCaptor.forClass(MigrationRecordLog.class);
        verify(recordLogRepository).record(captor.capture());

        assertEquals(MigrationItemStatus.CREATED, captor.getValue().getStatus());
        assertEquals(5, captor.getValue().getTargetEntityId());
        assertEquals("ror:123", captor.getValue().getSourceKey());
    }

    @Test
    public void shouldRetryUntilPolicyIsExhausted() {
        // given
        var run = newRun(MigrationEntityType.PERSON);
        var attempts = new AtomicInteger();

        var pipeline = new MigrationPipeline<>(SOURCE, MigrationEntityType.PERSON, String.class,
            pagedFetcher(List.of(List.of("retry-me"))),
            singleItemRouter((dto, migrationRun) -> {
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
            singleItemRouter((dto, migrationRun) -> {
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

        EntityCreator<String> creator = (dto, migrationRun) -> {
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
            .find(SOURCE, MigrationEntityType.JOURNAL_PUBLICATION, "cv-journal");
    }
}
