package rs.teslaris.assessment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.annotation.EntityClassificationEditCheck;
import rs.teslaris.assessment.service.interfaces.EntityAssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/entity-assessment-classification")
@RequiredArgsConstructor
public class EntityAssessmentClassificationController {

    private final EntityAssessmentClassificationService entityAssessmentClassificationService;


    @DeleteMapping("/{entityAssessmentClassificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION', 'EDIT_EVENT_ASSESSMENT_CLASSIFICATION')")
    @EntityClassificationEditCheck
    public void deleteEntityIndicatorProof(@PathVariable Integer entityAssessmentClassificationId) {
        entityAssessmentClassificationService.deleteEntityAssessmentClassification(
            entityAssessmentClassificationId);
    }
}
