package rs.teslaris.core.assessment.service.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.model.AssessmentMeasure;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface AssessmentMeasureService extends JPAService<AssessmentMeasure> {

    Page<AssessmentMeasureDTO> readAllAssessmentMeasures(Pageable pageable);

    AssessmentMeasureDTO readAssessmentMeasureById(Integer assessmentMeasureId);

    AssessmentMeasure createAssessmentMeasure(AssessmentMeasureDTO assessmentMeasureDTO);

    void updateAssessmentMeasure(Integer assessmentMeasureId,
                                 AssessmentMeasureDTO assessmentMeasureDTO);

    void deleteAssessmentMeasure(Integer assessmentMeasureId);
}