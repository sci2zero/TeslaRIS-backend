package rs.teslaris.core.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.AssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.AssessmentClassificationDTO;
import rs.teslaris.core.assessment.model.AssessmentClassification;
import rs.teslaris.core.assessment.repository.AssessmentClassificationRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentClassificationService;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.AssessmentClassificationReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
public class AssessmentClassificationServiceImpl extends JPAServiceImpl<AssessmentClassification>
    implements AssessmentClassificationService {

    private final AssessmentClassificationRepository assessmentClassificationRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    public Page<AssessmentClassificationDTO> readAllAssessmentClassifications(Pageable pageable) {
        return assessmentClassificationRepository.findAll(pageable)
            .map(AssessmentClassificationConverter::toDTO);
    }

    @Override
    protected JpaRepository<AssessmentClassification, Integer> getEntityRepository() {
        return assessmentClassificationRepository;
    }

    @Override
    public AssessmentClassificationDTO readAssessmentClassification(
        Integer assessmentClassificationId) {
        return AssessmentClassificationConverter.toDTO(findOne(assessmentClassificationId));
    }

    @Override
    public AssessmentClassification createAssessmentClassification(
        AssessmentClassificationDTO assessmentClassification) {
        var newAssessmentClassification = new AssessmentClassification();

        setCommonFields(newAssessmentClassification, assessmentClassification);

        return save(newAssessmentClassification);
    }

    @Override
    public void updateAssessmentClassification(Integer assessmentClassificationId,
                                               AssessmentClassificationDTO assessmentClassification) {
        var assessmentClassificationToUpdate = findOne(assessmentClassificationId);

        setCommonFields(assessmentClassificationToUpdate, assessmentClassification);

        save(assessmentClassificationToUpdate);
    }

    private void setCommonFields(AssessmentClassification assessmentClassification,
                                 AssessmentClassificationDTO assessmentClassificationDTO) {
        assessmentClassification.setFormalDescriptionOfRule(
            assessmentClassificationDTO.formalDescriptionOfRule());
        assessmentClassification.setCode(assessmentClassificationDTO.code());
        assessmentClassification.setTitle(
            multilingualContentService.getMultilingualContent(assessmentClassificationDTO.title()));
    }

    @Override
    public void deleteAssessmentClassification(Integer assessmentClassificationId) {
        if (assessmentClassificationRepository.isInUse(assessmentClassificationId)) {
            throw new AssessmentClassificationReferenceConstraintViolationException(
                "assessmentClassificationInUse.");
        }

        delete(assessmentClassificationId);
    }
}
