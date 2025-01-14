package rs.teslaris.core.assessment.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.EventAssessmentClassificationDTO;
import rs.teslaris.core.assessment.service.interfaces.EventAssessmentClassificationService;

@RestController
@RequestMapping("/api/assessment/event-assessment-classification")
@RequiredArgsConstructor
public class EventAssessmentClassificationController {

    private final EventAssessmentClassificationService eventAssessmentClassificationService;

    @GetMapping("/{eventId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        @PathVariable Integer eventId) {
        return eventAssessmentClassificationService.getAssessmentClassificationsForEvent(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION')")
    @Idempotent
    public EntityAssessmentClassificationResponseDTO createEventAssessmentClassification(
        @RequestBody @Valid EventAssessmentClassificationDTO eventAssessmentClassificationDTO) {
        var newEventAssessmentClassification =
            eventAssessmentClassificationService.createEventAssessmentClassification(
                eventAssessmentClassificationDTO);

        return EntityAssessmentClassificationConverter.toDTO(newEventAssessmentClassification);
    }

    @PutMapping("/{eventAssessmentClassificationId}")
    @PreAuthorize("hasAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEventAssessmentClassification(
        @RequestBody @Valid EventAssessmentClassificationDTO eventAssessmentClassificationDTO,
        @PathVariable Integer eventAssessmentClassificationId) {
        eventAssessmentClassificationService.updateEventAssessmentClassification(
            eventAssessmentClassificationId, eventAssessmentClassificationDTO);
    }
}
