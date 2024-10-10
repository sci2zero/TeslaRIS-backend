package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.PublicationSeriesAssessmentClassification;
import rs.teslaris.core.assessment.repository.PublicationSeriesAssessmentClassificationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class PublicationSeriesAssessmentClassificationJPAServiceImpl extends
    JPAServiceImpl<PublicationSeriesAssessmentClassification> {

    private final PublicationSeriesAssessmentClassificationRepository
        eventAssessmentClassificationRepository;

    @Autowired
    public PublicationSeriesAssessmentClassificationJPAServiceImpl(
        PublicationSeriesAssessmentClassificationRepository eventAssessmentClassificationRepository) {
        this.eventAssessmentClassificationRepository = eventAssessmentClassificationRepository;
    }

    @Override
    protected JpaRepository<PublicationSeriesAssessmentClassification, Integer> getEntityRepository() {
        return eventAssessmentClassificationRepository;
    }
}