package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.CommissionEditCheck;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.assessment.service.interfaces.AssessmentResearchAreaService;
import rs.teslaris.core.indexmodel.PersonIndex;

@RestController
@RequestMapping("/api/assessment/research-area")
@RequiredArgsConstructor
public class AssessmentResearchAreaController {

    private final AssessmentResearchAreaService assessmentResearchAreaService;

    @GetMapping
    public List<AssessmentResearchAreaDTO> readAssessmentResearchAreas() {
        return assessmentResearchAreaService.readAllAssessmentResearchAreas();
    }

    @GetMapping("/{personId}")
    public AssessmentResearchAreaDTO readPersonAssessmentResearchArea(
        @PathVariable Integer personId) {
        return assessmentResearchAreaService.readPersonAssessmentResearchArea(personId);
    }

    @GetMapping("/{researchAreaCode}/{commissionId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    public Page<PersonIndex> readPersonAssessmentResearchAreaForCommission(
        @PathVariable Integer commissionId,
        @PathVariable String researchAreaCode,
        Pageable pageable) {
        return assessmentResearchAreaService.readPersonAssessmentResearchAreaForCommission(
            commissionId, researchAreaCode, pageable);
    }

    @PatchMapping("/{personId}/{researchAreaCode}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RESEARCH_AREA')")
    @PersonEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public void setPersonAssessmentResearchArea(@PathVariable Integer personId,
                                                @PathVariable String researchAreaCode) {
        assessmentResearchAreaService.setPersonAssessmentResearchArea(personId, researchAreaCode);
    }

    @PatchMapping("/{personId}/{researchAreaCode}/{commissionId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public void setPersonAssessmentResearchAreaForCommission(@PathVariable Integer personId,
                                                             @PathVariable String researchAreaCode,
                                                             @PathVariable Integer commissionId) {
        assessmentResearchAreaService.setPersonAssessmentResearchAreaForCommission(personId,
            researchAreaCode, commissionId);
    }

    @DeleteMapping("/{personId}")
    @PreAuthorize("hasAuthority('EDIT_ASSESSMENT_RESEARCH_AREA')")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePersonAssessmentResearchArea(@PathVariable Integer personId) {
        assessmentResearchAreaService.deletePersonAssessmentResearchArea(personId);
    }

    @DeleteMapping("/{personId}/{commissionId}")
    @PreAuthorize("hasAuthority('UPDATE_COMMISSION')")
    @CommissionEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePersonAssessmentResearchAreaForCommission(@PathVariable Integer personId,
                                                                @PathVariable
                                                                Integer commissionId) {
        assessmentResearchAreaService.removePersonAssessmentResearchAreaForCommission(personId,
            commissionId);
    }

}
