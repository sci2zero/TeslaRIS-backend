package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EventAssessmentClassificationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class EventAssessmentClassificationJPAServiceImpl
    extends JPAServiceImpl<EventAssessmentClassification> {

    private final EventAssessmentClassificationRepository eventAssessmentClassificationRepository;

    @Autowired
    public EventAssessmentClassificationJPAServiceImpl(
        EventAssessmentClassificationRepository eventAssessmentClassificationRepository) {
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
    }

    @Override
    protected JpaRepository<EventAssessmentClassification, Integer> getEntityRepository() {
        return eventAssessmentClassificationRepository;
    }
}
