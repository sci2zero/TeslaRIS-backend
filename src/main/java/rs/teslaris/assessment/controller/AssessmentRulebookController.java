package rs.teslaris.assessment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.converter.AssessmentRulebookConverter;
import rs.teslaris.assessment.dto.AssessmentMeasureDTO;
import rs.teslaris.assessment.dto.AssessmentRulebookDTO;
import rs.teslaris.assessment.dto.AssessmentRulebookResponseDTO;
import rs.teslaris.assessment.service.interfaces.AssessmentRulebookService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;

@RestController
@RequestMapping("/api/assessment/assessment-rulebook")
@RequiredArgsConstructor
@Traceable
public class AssessmentRulebookController {

    private final AssessmentRulebookService assessmentRulebookService;


    @GetMapping
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    public Page<AssessmentRulebookResponseDTO> readAssessmentRulebooks(Pageable pageable,
                                                                       @RequestParam("lang")
                                                                       String language) {
        return assessmentRulebookService.readAllAssessmentRulebooks(pageable,
            language.toUpperCase());
    }

    @GetMapping("/{assessmentRulebookId}/measures")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    public Page<AssessmentMeasureDTO> readAssessmentMeasuresForRulebook(Pageable pageable,
                                                                        @PathVariable
                                                                        Integer assessmentRulebookId) {
        return assessmentRulebookService.readAssessmentMeasuresForRulebook(pageable,
            assessmentRulebookId);
    }

    @GetMapping("/{assessmentRulebookId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    public AssessmentRulebookResponseDTO readAssessmentRulebook(
        @PathVariable Integer assessmentRulebookId) {
        return assessmentRulebookService.readAssessmentRulebookById(assessmentRulebookId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
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

    @PatchMapping("/{assessmentRulebookId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @Idempotent
    DocumentFileResponseDTO addPDFFile(@PathVariable Integer assessmentRulebookId,
                                       @ModelAttribute @Valid DocumentFileDTO documentFile) {
        return assessmentRulebookService.addPDFFile(assessmentRulebookId, documentFile);
    }

    @DeleteMapping("/{assessmentRulebookId}/{documentFileId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void addPDFFile(@PathVariable Integer assessmentRulebookId,
                    @PathVariable Integer documentFileId) {
        assessmentRulebookService.deletePDFFile(assessmentRulebookId, documentFileId);
    }

    @PatchMapping("/set-default/{rulebookId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RULEBOOKS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setDefaultRulebook(@PathVariable Integer rulebookId) {
        assessmentRulebookService.setDefaultRulebook(rulebookId);
    }
}
