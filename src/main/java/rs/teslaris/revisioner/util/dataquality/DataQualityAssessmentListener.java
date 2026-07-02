package rs.teslaris.revisioner.util.dataquality;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.util.DataQualityCalculatorPtCris;
import rs.teslaris.revisioner.util.ObjectMapperProvider;

@Component
@RequiredArgsConstructor
public class DataQualityAssessmentListener {

    private final DataQualityCalculatorPtCris calculator;

    private final EntityRevisionRepository repository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DataQualityAssessmentEvent event) {
        calculator.assessDataQuality(event.entityRevision(), event.json(),
            ObjectMapperProvider.provideObjectmapper(), repository);
    }
}
