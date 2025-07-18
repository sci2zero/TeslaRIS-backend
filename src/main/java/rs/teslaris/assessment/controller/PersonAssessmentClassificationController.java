package rs.teslaris.assessment.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.dto.ResearcherAssessmentResponseDTO;
import rs.teslaris.assessment.dto.classification.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.service.interfaces.classification.PersonAssessmentClassificationService;
import rs.teslaris.core.annotation.Traceable;

@RestController
@RequestMapping("/api/assessment/person-assessment-classification")
@RequiredArgsConstructor
@Traceable
public class PersonAssessmentClassificationController {

    private final PersonAssessmentClassificationService personAssessmentClassificationService;

    @GetMapping("/{personId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPerson(
        @PathVariable Integer personId) {
        return personAssessmentClassificationService.getAssessmentClassificationsForPerson(
            personId);
    }

    @GetMapping("/assess/{personId}")
    public List<ResearcherAssessmentResponseDTO> assessResearcher(@PathVariable Integer personId,
                                                                  @RequestParam(value = "startDate", required = false)
                                                                  LocalDate startDate,
                                                                  @RequestParam(value = "endDate", required = false)
                                                                  LocalDate endDate) {
        if (Objects.isNull(startDate)) {
            startDate = LocalDate.of(1970, 1, 1);
        }
        if (Objects.isNull(endDate)) {
            endDate = LocalDate.now();
        }
        return personAssessmentClassificationService.assessSingleResearcher(personId, startDate,
            endDate);
    }
}
