package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.EventAssessmentClassification;
import rs.teslaris.core.assessment.repository.EventAssessmentClassificationRepository;
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
