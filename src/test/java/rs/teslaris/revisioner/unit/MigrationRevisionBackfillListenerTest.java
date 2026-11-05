package rs.teslaris.revisioner.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import rs.teslaris.core.applicationevent.MigrationRunFinishedEvent;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;
import rs.teslaris.revisioner.service.interfaces.RevisionService;
import rs.teslaris.revisioner.util.MigrationRevisionBackfillListener;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MigrationRevisionBackfillListenerTest {

    @Mock
    private RevisionService revisionService;

    @Mock
    private QualityAssessmentBackfillService qualityAssessmentBackfillService;

    @InjectMocks
    private MigrationRevisionBackfillListener listener;

    @Test
    public void shouldRecordANewMajorLineForEveryUpdatedEntity() {
        // given
        var event = new MigrationRunFinishedEvent("run-1", "hydrator", Set.of("PERSON"), null,
            Map.of("PERSON", List.of(7, 8)));

        // when
        listener.handle(event);

        // then
        verify(revisionService).recordRevisionFromCurrentState("PERSON", 7, null);
        verify(revisionService).recordRevisionFromCurrentState("PERSON", 8, null);
        verify(revisionService, never()).createRevisionFromCurrentState(any(), any(), any());
    }

    @Test
    public void shouldBackfillTheCreatedEntitiesAndRecordTheUpdatedOnesOfTheSameRun() {
        // given
        var event = new MigrationRunFinishedEvent("run-2", "hydrator", Set.of("PERSON"),
            Map.of("PERSON", List.of(1)), Map.of("PERSON", List.of(2)));

        // when
        listener.handle(event);

        // then
        verify(revisionService).createRevisionFromCurrentState("PERSON", 1, null);
        verify(revisionService).recordRevisionFromCurrentState("PERSON", 2, null);
        verify(qualityAssessmentBackfillService, never())
            .performBackfill(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    public void shouldSweepTheCreatedTypesWhenTheRunEnumeratedNothing() {
        // given
        var event = new MigrationRunFinishedEvent("run-3", "hydrator", Set.of("PERSON"), null,
            null);

        // when
        listener.handle(event);

        // then
        verify(qualityAssessmentBackfillService).performBackfill(
            eq(List.of(QualityAssessmentTarget.PERSON)), eq(List.of()), eq(List.of()), isNull(),
            eq(false));
        verify(revisionService, never()).recordRevisionFromCurrentState(any(), any(), any());
    }
}
