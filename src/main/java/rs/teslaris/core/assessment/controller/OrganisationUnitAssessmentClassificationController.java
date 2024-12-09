package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.OrganisationUnitAssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/organisation-unit-assessment-classification")
@RequiredArgsConstructor
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
