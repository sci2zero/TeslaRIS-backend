package rs.teslaris.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.service.interfaces.classification.OrganisationUnitAssessmentClassificationService;
import rs.teslaris.core.annotation.Traceable;

@RestController
@RequestMapping("/api/assessment/organisation-unit-assessment-classification")
@RequiredArgsConstructor
@Traceable
public class OrganisationUnitAssessmentClassificationController {

    private final OrganisationUnitAssessmentClassificationService
        organisationUnitAssessmentClassificationService;

    @GetMapping("/{organisationUnitId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForOrganisationUnit(
        @PathVariable Integer organisationUnitId) {
        return organisationUnitAssessmentClassificationService.getAssessmentClassificationsForOrganisationUnit(
            organisationUnitId);
    }
}
