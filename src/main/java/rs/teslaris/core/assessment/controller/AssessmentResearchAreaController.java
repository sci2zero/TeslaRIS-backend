package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.assessment.dto.AssessmentResearchAreaDTO;
import rs.teslaris.core.assessment.service.interfaces.AssessmentResearchAreaService;

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

    @PatchMapping("/{personId}/{researchAreaCode}")
    @PersonEditCheck
    @Idempotent
    @ResponseStatus(HttpStatus.CREATED)
    public void setPersonAssessmentResearchArea(@PathVariable Integer personId,
                                                @PathVariable String researchAreaCode) {
        assessmentResearchAreaService.setPersonAssessmentResearchArea(personId, researchAreaCode);
    }

    @DeleteMapping("/{personId}")
    @PersonEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePersonAssessmentResearchArea(@PathVariable Integer personId) {
        assessmentResearchAreaService.deletePersonAssessmentResearchArea(personId);
    }

}
