package rs.teslaris.core.assessment.service.impl;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.DocumentAssessmentClassification;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.assessment.model.EventAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.EntityAssessmentClassificationService;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class EntityAssessmentClassificationServiceImpl
    extends JPAServiceImpl<EntityAssessmentClassification> implements
    EntityAssessmentClassificationService {

    protected final AssessmentClassificationService assessmentClassificationService;

    protected final CommissionService commissionService;

    protected final DocumentPublicationService documentPublicationService;

    protected final ConferenceService conferenceService;

    private final EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;


    @Override
    public void deleteEntityAssessmentClassification(Integer entityAssessmentId) {
        var entityAssessmentClassificationToDelete = findOne(entityAssessmentId);
        entityAssessmentClassificationRepository.delete(entityAssessmentClassificationToDelete);

        if (entityAssessmentClassificationToDelete instanceof DocumentAssessmentClassification) {
            documentPublicationService.reindexDocumentVolatileInformation(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete).getDocument()
                    .getId());
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
