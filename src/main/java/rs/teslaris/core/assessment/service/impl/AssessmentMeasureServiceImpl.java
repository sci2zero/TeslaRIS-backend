package rs.teslaris.core.assessment.service.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.assessment.converter.AssessmentMeasureConverter;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.assessment.repository.AssessmentMeasureRepository;
import rs.teslaris.core.assessment.service.interfaces.AssessmentMeasureService;
import rs.teslaris.core.assessment.service.interfaces.AssessmentRulebookService;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;

@Service
@RequiredArgsConstructor
@Transactional
public class AssessmentMeasureServiceImpl extends JPAServiceImpl<AssessmentMeasure> implements
    AssessmentMeasureService {

    private final AssessmentMeasureRepository assessmentMeasureRepository;

    private final AssessmentRulebookService assessmentRulebookService;

    private final MultilingualContentService multilingualContentService;


    @Override
    public Page<AssessmentMeasureDTO> searchAssessmentMeasures(Pageable pageable,
                                                               String searchExpression) {
        return assessmentMeasureRepository.searchAssessmentMeasures(pageable, searchExpression)
            .map(AssessmentMeasureConverter::toDTO);
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

        var rulebook =
            assessmentRulebookService.findOne(assessmentMeasureDTO.assessmentRulebookId());
        assessmentMeasure.setRulebook(rulebook);
        if (!rulebook.getAssessmentMeasures().contains(assessmentMeasure)) {
            rulebook.getAssessmentMeasures().add(assessmentMeasure);
        }
    }

    @Override
    public void deleteAssessmentMeasure(Integer assessmentMeasureId) {
        var assessmentMeasureToDelete = findOne(assessmentMeasureId);

        if (Objects.nonNull(assessmentMeasureToDelete.getRulebook())) {
            assessmentMeasureToDelete.getRulebook().getAssessmentMeasures()
                .remove(assessmentMeasureToDelete);
        }

        delete(assessmentMeasureId);
    }
}
