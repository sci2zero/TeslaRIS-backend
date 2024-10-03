package rs.teslaris.core.assessment.controller;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.converter.AssessmentRulebookConverter;
import rs.teslaris.core.assessment.dto.AssessmentRulebookDTO;
import rs.teslaris.core.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.AssessmentRulebookService;

@RestController
@RequestMapping("/api/assessment/assessment-rulebook")
@RequiredArgsConstructor
public class AssessmentRulebookController {

    private final AssessmentRulebookService assessmentRulebookService;


    @GetMapping
    public Page<AssessmentRulebookResponseDTO> readAssessmentRulebooks(Pageable pageable) {
        return assessmentRulebookService.readAllAssessmentRulebooks(pageable);
    }

    @GetMapping("/{assessmentRulebookId}")
    public AssessmentRulebookResponseDTO readAssessmentRulebook(
        @PathVariable Integer assessmentRulebookId) {
        return assessmentRulebookService.readAssessmentRulebookById(assessmentRulebookId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @ResponseStatus(HttpStatus.CREATED)
    @Idempotent
    public AssessmentRulebookResponseDTO createAssessmentRulebook(
        @RequestBody AssessmentRulebookDTO assessmentRulebookDTO) {
        var createdAssessmentRulebook =
            assessmentRulebookService.createAssessmentRulebook(assessmentRulebookDTO);

        return AssessmentRulebookConverter.toDTO(createdAssessmentRulebook);
    }

    @PutMapping("/{assessmentRulebookId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAssessmentRulebook(@RequestBody AssessmentRulebookDTO assessmentRulebookDTO,
                                         @PathVariable Integer assessmentRulebookId) {
        assessmentRulebookService.updateAssessmentRulebook(assessmentRulebookId,
            assessmentRulebookDTO);
    }

    @DeleteMapping("/{assessmentRulebookId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssessmentRulebook(@PathVariable Integer assessmentRulebookId) {
        assessmentRulebookService.deleteAssessmentRulebook(assessmentRulebookId);
    }
}
