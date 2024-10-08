package rs.teslaris.core.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
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
}
