package rs.teslaris.core.service.impl.assessment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.assessment.AssessmentClassificationConverter;
import rs.teslaris.core.dto.assessment.AssessmentClassificationDTO;
import rs.teslaris.core.model.assessment.AssessmentClassification;
import rs.teslaris.core.repository.assessment.AssessmentClassificationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.assessment.AssessmentClassificationService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@Service
@RequiredArgsConstructor
public class AssessmentClassificationServiceImpl extends JPAServiceImpl<AssessmentClassification>
    implements AssessmentClassificationService {

    private final AssessmentClassificationRepository assessmentClassificationRepository;

    private final MultilingualContentService multilingualContentService;


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
    public AssessmentClassification createAssessmentCLassification(
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
        delete(assessmentClassificationId);
    }
}
