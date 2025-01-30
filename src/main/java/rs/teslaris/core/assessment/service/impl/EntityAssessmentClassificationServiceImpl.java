package rs.teslaris.core.assessment.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.EntityAssessmentClassification;
import rs.teslaris.core.assessment.repository.EntityAssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.assessment.service.interfaces.CommissionService;
import rs.teslaris.core.assessment.service.interfaces.EntityAssessmentClassificationService;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Service
@Primary
@RequiredArgsConstructor
public class EntityAssessmentClassificationServiceImpl
    extends JPAServiceImpl<EntityAssessmentClassification> implements
    EntityAssessmentClassificationService {

    protected final AssessmentClassificationService assessmentClassificationService;

    protected final CommissionService commissionService;

    private final EntityAssessmentClassificationRepository entityAssessmentClassificationRepository;


    @Override
    public void deleteEntityAssessmentClassification(Integer entityAssessmentId) {
        entityAssessmentClassificationRepository.delete(findOne(entityAssessmentId));
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

        if (Objects.nonNull(dto.getCommissionId())) {
            entityAssessmentClassification.setCommission(
                commissionService.findOne(dto.getCommissionId()));
        }

        entityAssessmentClassification.setAssessmentClassification(
            assessmentClassificationService.findOne(dto.getAssessmentClassificationId()));
    }
}
