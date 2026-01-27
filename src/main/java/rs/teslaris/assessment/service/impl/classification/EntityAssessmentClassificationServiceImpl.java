package rs.teslaris.assessment.service.impl.classification;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationDTO;
import rs.teslaris.assessment.model.classification.DocumentAssessmentClassification;
import rs.teslaris.assessment.model.classification.EntityAssessmentClassification;
import rs.teslaris.assessment.model.classification.EventAssessmentClassification;
import rs.teslaris.assessment.model.classification.PublicationSeriesAssessmentClassification;
import rs.teslaris.assessment.model.indicator.ApplicableEntityType;
import rs.teslaris.assessment.repository.classification.EntityAssessmentClassificationRepository;
import rs.teslaris.assessment.service.interfaces.CommissionService;
import rs.teslaris.assessment.service.interfaces.classification.AssessmentClassificationService;
import rs.teslaris.assessment.service.interfaces.classification.EntityAssessmentClassificationService;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.applicationevent.EntityAssessmentChanged;
import rs.teslaris.core.applicationevent.ResearcherPointsReindexingEvent;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.Monograph;
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
        var commissionId = entityAssessmentClassificationToDelete.getCommission().getId();

        entityAssessmentClassificationRepository.delete(entityAssessmentClassificationToDelete);

        ApplicableEntityType entityType = null;
        Integer entityId = null;

        if (entityAssessmentClassificationToDelete instanceof DocumentAssessmentClassification) {
            if (Hibernate.getClass(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete)
                    .getDocument()) == Monograph.class) {
                entityType = ApplicableEntityType.MONOGRAPH;
                entityId =
                    ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete)
                        .getDocument().getId();
            }

            documentPublicationService.reindexDocumentVolatileInformation(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete).getDocument()
                    .getId());
            applicationEventPublisher.publishEvent(new ResearcherPointsReindexingEvent(
                ((DocumentAssessmentClassification) entityAssessmentClassificationToDelete).getDocument()
                    .getContributors().stream().filter(c -> Objects.nonNull(c.getPerson()))
                    .map(c -> c.getPerson().getId()).toList()));
        } else if (entityAssessmentClassificationToDelete instanceof EventAssessmentClassification) {
            entityType = ApplicableEntityType.EVENT;
            entityId =
                ((EventAssessmentClassification) entityAssessmentClassificationToDelete).getEvent()
                    .getId();

            conferenceService.reindexVolatileConferenceInformation(
                ((EventAssessmentClassification) entityAssessmentClassificationToDelete).getEvent()
                    .getId());
        } else if (entityAssessmentClassificationToDelete instanceof PublicationSeriesAssessmentClassification) {
            if (Hibernate.getClass(
                ((PublicationSeriesAssessmentClassification) entityAssessmentClassificationToDelete)
                    .getPublicationSeries()) == Journal.class) {
                entityType = ApplicableEntityType.PUBLICATION_SERIES;
                entityId =
                    ((PublicationSeriesAssessmentClassification) entityAssessmentClassificationToDelete)
                        .getPublicationSeries().getId();
            }
        }

        if (Objects.nonNull(entityType) && Objects.nonNull(entityId)) {
            applicationEventPublisher.publishEvent(
                new EntityAssessmentChanged(entityType, entityId, commissionId, true));
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
