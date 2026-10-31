package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.model.user.Authority;
import rs.teslaris.core.model.user.User;
import rs.teslaris.core.revision.DataQualityFixator;
import rs.teslaris.core.revision.RevisionRestorer;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;
import rs.teslaris.revisioner.repository.DataQualityFixatorPipelineRepository;
import rs.teslaris.revisioner.service.impl.DataQualityFixatorRunServiceImpl;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.RevisionRestorerRegistry;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorRegistry;

@SpringBootTest
public class DataQualityFixatorRunServiceTest {

    private static final int BATCH_SIZE = 1000;

    private static final Integer USER_ID = 5;

    @Mock
    private DataQualityFixatorPipelineRepository dataQualityFixatorPipelineRepository;

    @Mock
    private DataQualityFixatorRegistry dataQualityFixatorRegistry;

    @Mock
    private RevisionRestorerRegistry revisionRestorerRegistry;

    @Mock
    private RevisionService revisionService;

    @Mock
    private SearchService<DataQualityAssessmentIndex> searchService;

    @Mock
    private TaskManagerService taskManagerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DataQualityFixatorRunServiceImpl dataQualityFixatorRunService;

    private StubFixator fixator;

    private StubRestorer restorer;

    @BeforeEach
    public void setUp() {
        fixator = new StubFixator();
        restorer = new StubRestorer();

        var authority = new Authority("ADMIN", new HashSet<>());
        var user = new User();
        user.setAuthority(authority);

        lenient().when(userService.findOne(USER_ID)).thenReturn(user);
    }

    private DataQualityFixatorPipeline pipeline(List<String> remarkKeys,
                                                List<String> appliedFixators,
                                                boolean createVersionOnEveryStep) {
        var pipeline = new DataQualityFixatorPipeline();
        pipeline.setPipelineName("titles");
        pipeline.setProfileName("ptcris");
        pipeline.setVersion(1);
        pipeline.setMatchedRemarkKeys(remarkKeys);
        pipeline.setAppliedFixators(appliedFixators);
        pipeline.setCreateVersionOnEveryStep(createVersionOnEveryStep);

        return pipeline;
    }

    private DataQualityAssessmentIndex assessment(String entityType, Integer entityId) {
        var index = new DataQualityAssessmentIndex();
        index.setId(entityType + entityId + System.nanoTime());
        index.setEntityType(entityType);
        index.setEntityId(entityId);

        return index;
    }

    private void stubPipeline(DataQualityFixatorPipeline pipeline) {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline));
        when(dataQualityFixatorRegistry.get("typographicCharacter")).thenReturn(
            Optional.of(fixator));
    }

    private void stubBatches(List<List<DataQualityAssessmentIndex>> batches) {
        var remaining = new ArrayList<>(batches);

        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString(), any())).thenAnswer(
            invocation -> new PageImpl<>(remaining.isEmpty() ? List.of() : remaining.remove(0)));
    }

    @Test
    public void shouldFixEveryMatchedRecordOnce() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        stubBatches(List.of(List.of(assessment("PUBLISHER", 1), assessment("PUBLISHER", 2))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, times(2)).applyFixedState(eq("PUBLISHER"), any(), any(),
            eq("revisionFixator:titles"));
    }

    @Test
    public void shouldNotWriteWhenNoFixatorChangesAnything() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        fixator.mutate = false;
        stubBatches(List.of(List.of(assessment("PUBLISHER", 1))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, never()).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldSkipPipelineThatMatchesNoRemarkKey() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline(List.of(), List.of("typographicCharacter"), false)));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(searchService, never()).runQueryWithoutTotal(any(), any(), any(), anyString(),
            any());
    }

    @Test
    public void shouldSkipPipelineThatAppliesNoFixator() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline(List.of("titleTooLong"), List.of(), false)));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(searchService, never()).runQueryWithoutTotal(any(), any(), any(), anyString(),
            any());
    }

    @Test
    public void shouldSkipRecordWithoutRestorer() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("FUNDING")).thenReturn(Optional.empty());

        stubBatches(List.of(List.of(assessment("FUNDING", 1))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, never()).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldSkipRecordWhoseCurrentStateIsUnreadable() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        restorer.readable = false;
        stubBatches(List.of(List.of(assessment("PUBLISHER", 1))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, never()).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldKeepFixingAfterOneRecordFails() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        stubBatches(List.of(List.of(assessment("PUBLISHER", 1), assessment("PUBLISHER", 2))));

        doThrowOnFirstWrite();

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, times(2)).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldFixRecordSharingTheWatermarkOfABatchBoundaryOnlyOnce() {
        stubPipeline(pipeline(List.of("titleTooLong"), List.of("typographicCharacter"), false));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));
        when(revisionRestorerRegistry.get("JOURNAL")).thenReturn(Optional.empty());

        var firstBatch = new ArrayList<DataQualityAssessmentIndex>();

        for (var counter = 1; counter < BATCH_SIZE; counter++) {
            firstBatch.add(assessment("PUBLISHER", counter));
        }

        var boundary = assessment("PUBLISHER", BATCH_SIZE);
        firstBatch.add(boundary);

        // The inclusive watermark serves the boundary id again, together with the record of
        // another entity type that shares it.
        stubBatches(
            List.of(firstBatch, List.of(boundary, assessment("JOURNAL", BATCH_SIZE))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, times(BATCH_SIZE)).applyFixedState(eq("PUBLISHER"), any(), any(),
            any());
    }

    @Test
    public void shouldWriteOncePerChangingFixatorWhenEveryStepIsVersioned() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline(List.of("titleTooLong"),
                List.of("typographicCharacter", "identifierAffix"), true)));
        when(dataQualityFixatorRegistry.get("typographicCharacter")).thenReturn(
            Optional.of(fixator));
        when(dataQualityFixatorRegistry.get("identifierAffix")).thenReturn(
            Optional.of(new StubFixator()));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        stubBatches(List.of(List.of(assessment("PUBLISHER", 1))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService, times(2)).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldIgnoreUnknownFixatorKeyAndRunTheRest() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("titles")).thenReturn(
            Optional.of(pipeline(List.of("titleTooLong"),
                List.of("ghostFixator", "typographicCharacter"), false)));
        when(dataQualityFixatorRegistry.get("ghostFixator")).thenReturn(Optional.empty());
        when(dataQualityFixatorRegistry.get("typographicCharacter")).thenReturn(
            Optional.of(fixator));
        when(revisionRestorerRegistry.get("PUBLISHER")).thenReturn(Optional.of(restorer));

        stubBatches(List.of(List.of(assessment("PUBLISHER", 1))));

        dataQualityFixatorRunService.runPipeline("titles", USER_ID);

        verify(revisionService).applyFixedState(any(), any(), any(), any());
    }

    @Test
    public void shouldDoNothingForUnknownPipeline() {
        when(dataQualityFixatorPipelineRepository.findByPipelineName("ghost")).thenReturn(
            Optional.empty());

        dataQualityFixatorRunService.runPipeline("ghost", USER_ID);

        verify(searchService, never()).runQueryWithoutTotal(any(), any(), any(), anyString(),
            any());
    }

    @Test
    public void shouldScheduleRunOfOnePipelineAndSaveItsMetadata() {
        var timeToRun = Instant.now().plusSeconds(600);

        when(taskManagerService.scheduleTask(anyString(), any(), any(), any(), any())).thenReturn(
            "Data_Quality_Fix-titles-1");

        dataQualityFixatorRunService.scheduleFixatorRun(timeToRun, "titles", USER_ID,
            RecurrenceType.DAILY);

        var captor = ArgumentCaptor.forClass(ScheduledTaskMetadata.class);
        verify(taskManagerService).saveTaskMetadata(captor.capture());

        assertEquals(ScheduledTaskType.DATA_QUALITY_FIX, captor.getValue().getType());
        assertEquals("titles", captor.getValue().getMetadata().get("pipelineName"));
        assertEquals(USER_ID, captor.getValue().getMetadata().get("userId"));
        assertEquals(RecurrenceType.DAILY, captor.getValue().getRecurrenceType());
    }

    @Test
    public void shouldScheduleRunOfEveryPipelineWhenNoneIsNamed() {
        var timeToRun = Instant.now().plusSeconds(600);

        when(taskManagerService.scheduleTask(anyString(), any(), any(), any(), any())).thenReturn(
            "Data_Quality_Fix-ALL-1");

        dataQualityFixatorRunService.scheduleFixatorRun(timeToRun, null, USER_ID,
            RecurrenceType.ONCE);

        var taskIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(taskManagerService).scheduleTask(taskIdCaptor.capture(), any(), any(), any(),
            any());

        assertTrue(taskIdCaptor.getValue().startsWith("Data_Quality_Fix-ALL-"));
    }

    private void doThrowOnFirstWrite() {
        var failed = new boolean[1];

        doAnswer(invocation -> {
            if (!failed[0]) {
                failed[0] = true;
                throw new IllegalStateException("duplicate identifier");
            }

            return null;
        }).when(revisionService).applyFixedState(any(), any(), any(), any());
    }

    private static final class StubFixator implements DataQualityFixator {

        private boolean mutate = true;

        @Override
        public String key() {
            return "stub";
        }

        @Override
        public String name() {
            return "Stub";
        }

        @Override
        public void applyFix(PublisherDTO publisher) {
            if (mutate) {
                publisher.setName(
                    List.of(new MultilingualContentDTO(1, "EN", "Fixed", 1)));
            }
        }
    }

    private static final class StubRestorer implements RevisionRestorer<PublisherDTO> {

        private boolean readable = true;

        @Override
        public String entityType() {
            return "PUBLISHER";
        }

        @Override
        public Class<PublisherDTO> dtoClass() {
            return PublisherDTO.class;
        }

        @Override
        public void restore(Integer entityId, PublisherDTO dto) {
        }

        @Override
        public Object readCurrentState(Integer entityId) {
            if (!readable) {
                return null;
            }

            var publisher = new PublisherDTO();
            publisher.setId(entityId);
            publisher.setName(
                List.of(new MultilingualContentDTO(1, "EN", "Unfixed", 1)));

            return publisher;
        }
    }
}
