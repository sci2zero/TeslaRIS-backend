package rs.teslaris.core.assessment.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.PersonEditCheck;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.core.assessment.service.interfaces.PersonAssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/person-assessment-classification")
@RequiredArgsConstructor
public class PersonAssessmentClassificationController {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    @GetMapping("/{personId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        @PathVariable Integer personId) {
        return personAssessmentClassificationService.getAssessmentClassificationsForPerson(
            personId);
    }

    @GetMapping("/assess/{personId}")
    @PersonEditCheck
    public List<ResearcherAssessmentResponseDTO> assessResearcher(@PathVariable Integer personId) {
        return personAssessmentClassificationService.assessSingleResearcher(personId, 1);
    }

    // TODO: remove, only for testing
    @PostMapping("/assess")
    public void assessResearchers() {
        personAssessmentClassificationService.assessResearchers(LocalDate.of(2025, 1, 1), 5, 1,
            new ArrayList<>(), new ArrayList<>());
    }
}
