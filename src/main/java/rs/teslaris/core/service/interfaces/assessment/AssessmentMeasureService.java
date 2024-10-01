package rs.teslaris.core.service.interfaces.assessment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.assessment.AssessmentMeasureDTO;
import rs.teslaris.core.model.assessment.AssessmentMeasure;
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
