package rs.teslaris.core.controller.assessment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.converter.assessment.AssessmentMeasureConverter;
import rs.teslaris.core.dto.assessment.AssessmentMeasureDTO;
import rs.teslaris.core.service.interfaces.assessment.AssessmentMeasureService;

@RestController
@RequestMapping("api/assessment/assessment-measure")
@RequiredArgsConstructor
public class AssessmentMeasureController {

    private final AssessmentMeasureService assessmentMeasureService;


    @GetMapping
    public Page<AssessmentMeasureDTO> readAssessmentMeasures(Pageable pageable) {
        return assessmentMeasureService.readAllAssessmentMeasures(pageable);
    }

    @GetMapping("/{assessmentMeasureId}")
    public AssessmentMeasureDTO readAssessmentMeasure(@PathVariable Integer assessmentMeasureId) {
        return assessmentMeasureService.readAssessmentMeasureById(assessmentMeasureId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public AssessmentMeasureDTO createAssessmentMeasure(
        @RequestBody AssessmentMeasureDTO assessmentMeasureDTO) {
        var createdAssessmentMeasure =
            assessmentMeasureService.createAssessmentMeasure(
                assessmentMeasureDTO);

        return AssessmentMeasureConverter.toDTO(createdAssessmentMeasure);
    }

    @PutMapping("/{assessmentMeasureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssessmentMeasure(@RequestBody AssessmentMeasureDTO assessmentMeasureDTO,
                                        @PathVariable Integer assessmentMeasureId) {
        assessmentMeasureService.updateAssessmentMeasure(assessmentMeasureId,
            assessmentMeasureDTO);
    }

    @DeleteMapping("/{assessmentMeasureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessmentMeasure(@PathVariable Integer assessmentMeasureId) {
        assessmentMeasureService.deleteAssessmentMeasure(assessmentMeasureId);
    }
}
