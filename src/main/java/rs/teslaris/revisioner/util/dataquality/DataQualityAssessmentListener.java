package rs.teslaris.revisioner.util.dataquality;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.util.DataQualityCalculatorPtCris;
import rs.teslaris.revisioner.util.ObjectMapperProvider;

@Component
@RequiredArgsConstructor
public class DataQualityAssessmentListener {

    private final DataQualityCalculatorPtCris calculator;

    private final EntityRevisionRepository entityRevisionRepository;

    private final DataQualityAssessmentRepository repository;


    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DataQualityAssessmentEvent event) {
        var ptCrisAssessment = DataQualityAssessment
            .builder()
            .revision(event.entityRevision())
            .engineVersion("1.0.0")
            .startedAt(Instant.now())
            .build();

        event.entityRevision().addAssessment(ptCrisAssessment);

        calculator.assessDataQuality(ptCrisAssessment, event.json(),
            ObjectMapperProvider.provideObjectmapper(), repository);

        entityRevisionRepository.save(event.entityRevision());
    }
}
