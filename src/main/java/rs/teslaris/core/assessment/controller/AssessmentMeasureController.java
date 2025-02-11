package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.converter.AssessmentMeasureConverter;
import rs.teslaris.core.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.core.assessment.service.interfaces.AssessmentMeasureService;

@RestController
@RequestMapping("api/assessment/assessment-measure")
@RequiredArgsConstructor
public class AssessmentMeasureController {

    private final AssessmentMeasureService assessmentMeasureService;


    @GetMapping
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    public Page<AssessmentMeasureDTO> searchAssessmentMeasures(
        @RequestParam String searchExpression,
        Pageable pageable) {
        return assessmentMeasureService.searchAssessmentMeasures(pageable, searchExpression);
    }

    @GetMapping("/{assessmentMeasureId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    public AssessmentMeasureDTO readAssessmentMeasure(@PathVariable Integer assessmentMeasureId) {
        return assessmentMeasureService.readAssessmentMeasureById(assessmentMeasureId);
    }

    @GetMapping("/point-rules")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    public List<String> readPointRules() {
        return assessmentMeasureService.listAllPointRules();
    }

    @GetMapping("/scaling-rules")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    public List<String> readScalingRules() {
        return assessmentMeasureService.listAllScalingRules();
    }

    @GetMapping("/assessment-groups")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    public List<String> readAssessmentGroups() {
        return assessmentMeasureService.listAllGroupCodes();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
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
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssessmentMeasure(@RequestBody AssessmentMeasureDTO assessmentMeasureDTO,
                                        @PathVariable Integer assessmentMeasureId) {
        assessmentMeasureService.updateAssessmentMeasure(assessmentMeasureId,
            assessmentMeasureDTO);
    }

    @DeleteMapping("/{assessmentMeasureId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_MEASURES')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessmentMeasure(@PathVariable Integer assessmentMeasureId) {
        assessmentMeasureService.deleteAssessmentMeasure(assessmentMeasureId);
    }
}
