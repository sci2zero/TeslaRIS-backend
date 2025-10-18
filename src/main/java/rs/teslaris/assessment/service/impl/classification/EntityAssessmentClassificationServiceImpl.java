package rs.teslaris.assessment.service.impl.classification;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.EntityAssessmentClassificationService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
@Traceable
public class EntityAssessmentClassificationServiceImpl
    extends JPAServiceImpl<EntityAssessmentClassification> implements
    EntityAssessmentClassificationService {

    protected final AssessmentClassificationService assessmentClassificationService;

    protected final CommissionService commissionService;

    protected final DocumentPublicationService documentPublicationService;

    protected final ConferenceService conferenceService;

    protected final ApplicationEventPublisher applicationEventPublisher;

    private final EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;


    @Override
    public void deleteEntityAssessmentClassification(Integer entityAssessmentId) {
        var entityAssessmentClassificationToDelete = findOne(entityAssessmentId);
        entityAssessmentClassificationRepository.delete(entityAssessmentClassificationToDelete);

        if (entityAssessmentClassificationToDelete instanceof DocumentAssessmentClassification) {
            documentPublicationService.reindexDocumentVolatileInformation(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete).getDocument()
                    .getId());
            applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete).getDocument()
                    .getContributors().stream().filter(c -> Objects.nonNull(c.getPerson()))
                    .map(c -> c.getPerson().getId()).toList()));
        } else if (entityAssessmentClassificationToDelete instanceof EventAssessmentClassification) {
            conferenceService.reindexVolatileConferenceInformation(
                ((EventAssessmentClassification) entityAssessmentClassificationToDelete).getEvent()
                    .getId());
        }
    }

    @Override
    protected JpaRepository<EntityAssessmentClassification, Integer> getEntityRepository() {
        return entityAssessmentClassificationRepository;
    }

    protected void setCommonFields(EntityAssessmentClassification entityAssessmentClassification,
                                   EntityAssessmentClassificationDTO dto) {
        entityAssessmentClassification.setTimestamp(LocalDateTime.now());
        entityAssessmentClassification.setManual(true);
        entityAssessmentClassification.setClassificationYear(dto.getClassificationYear());

        entityAssessmentClassification.setAssessmentClassification(
            assessmentClassificationService.findOne(dto.getAssessmentClassificationId()));
    }
}
