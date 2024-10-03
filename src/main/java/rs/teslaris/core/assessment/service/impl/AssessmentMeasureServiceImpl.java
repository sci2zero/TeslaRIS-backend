package rs.teslaris.core.assessment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.converter.AssessmentMeasureConverter;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentMeasureService;
import rs.teslaris.core.service.impl.JPAServiceImpl;
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
