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
import rs.teslaris.core.converter.assessment.AssessmentClassificationConverter;
import rs.teslaris.core.dto.assessment.AssessmentClassificationDTO;
import rs.teslaris.core.service.interfaces.assessment.AssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/assessment-classification")
@RequiredArgsConstructor
public class AssessmentClassificationController {

    private final AssessmentClassificationService assessmentClassificationService;


    @GetMapping
    public Page<AssessmentClassificationDTO> readAssessmentClassifications(Pageable pageable) {
        return assessmentClassificationService.readAllAssessmentClassifications(pageable);
    }

    @GetMapping("/{assessmentClassificationId}")
    public AssessmentClassificationDTO readAssessmentClassification(
        @PathVariable Integer assessmentClassificationId) {
        return assessmentClassificationService.readAssessmentClassification(
            assessmentClassificationId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssessmentClassificationDTO createAssessmentClassification(
        @RequestBody AssessmentClassificationDTO assessmentClassificationDTO) {
        var createdAssessmentClassification =
            assessmentClassificationService.createAssessmentClassification(
                assessmentClassificationDTO);

        return AssessmentClassificationConverter.toDTO(createdAssessmentClassification);
    }

    @PutMapping("/{assessmentClassificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssessmentClassification(
        @RequestBody AssessmentClassificationDTO assessmentClassificationDTO,
        @PathVariable Integer assessmentClassificationId) {
        assessmentClassificationService.updateAssessmentClassification(assessmentClassificationId,
            assessmentClassificationDTO);
    }

    @DeleteMapping("/{assessmentClassificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessmentClassification(@PathVariable Integer assessmentClassificationId) {
        assessmentClassificationService.deleteAssessmentClassification(assessmentClassificationId);
    }
}
