package rs.teslaris.core.service.impl.assessment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.converter.assessment.AssessmentMeasureConverter;
import rs.teslaris.core.dto.assessment.AssessmentMeasureDTO;
import rs.teslaris.core.model.assessment.AssessmentMeasure;
import rs.teslaris.core.repository.assessment.AssessmentMeasureRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.assessment.AssessmentMeasureService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.util.exceptionhandling.exception.AssessmentMeasureReferenceConstraintViolationException;

@Service
@RequiredArgsConstructor
public class AssessmentMeasureServiceImpl extends JPAServiceImpl<AssessmentMeasure> implements
    AssessmentMeasureService {

    private final AssessmentMeasureRepository assessmentMeasureRepository;

    private final MultilingualContentService multilingualContentService;


    @Override
    public Page<AssessmentMeasureDTO> readAllAssessmentMeasures(Pageable pageable) {
        return assessmentMeasureRepository.findAll(pageable).map(AssessmentMeasureConverter::toDTO);
    }

    @Override
    protected JpaRepository<AssessmentMeasure, Integer> getEntityRepository() {
        return assessmentMeasureRepository;
    }

    @Override
    public AssessmentMeasureDTO readAssessmentMeasureById(
        Integer assessmentMeasureId) {
        return AssessmentMeasureConverter.toDTO(findOne(assessmentMeasureId));
    }

    @Override
    public AssessmentMeasure createAssessmentMeasure(
        AssessmentMeasureDTO assessmentMeasure) {
        var newAssessmentMeasure = new AssessmentMeasure();

        setCommonFields(newAssessmentMeasure, assessmentMeasure);

        return save(newAssessmentMeasure);
    }

    @Override
    public void updateAssessmentMeasure(Integer assessmentMeasureId,
                                        AssessmentMeasureDTO assessmentMeasure) {
        var assessmentMeasureToUpdate = findOne(assessmentMeasureId);

        setCommonFields(assessmentMeasureToUpdate, assessmentMeasure);

        save(assessmentMeasureToUpdate);
    }

    private void setCommonFields(AssessmentMeasure assessmentMeasure,
                                 AssessmentMeasureDTO assessmentMeasureDTO) {
        assessmentMeasure.setFormalDescriptionOfRule(
            assessmentMeasureDTO.formalDescriptionOfRule());
        assessmentMeasure.setCode(assessmentMeasureDTO.code());
        assessmentMeasure.setValue(assessmentMeasureDTO.value());
        assessmentMeasure.setTitle(
            multilingualContentService.getMultilingualContent(assessmentMeasureDTO.title()));
    }

    @Override
    public void deleteAssessmentMeasure(Integer assessmentMeasureId) {
        if (assessmentMeasureRepository.isInUse(assessmentMeasureId)) {
            throw new AssessmentMeasureReferenceConstraintViolationException(
                "assessmentMeasureInUse.");
        }

        delete(assessmentMeasureId);
    }
}
