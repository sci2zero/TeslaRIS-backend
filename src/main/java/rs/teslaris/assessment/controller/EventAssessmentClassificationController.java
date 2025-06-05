package rs.teslaris.assessment.controller;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.annotation.EntityClassificationEditCheck;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.EventAssessmentClassificationDTO;
import rs.teslaris.assessment.service.interfaces.EventAssessmentClassificationService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/event-assessment-classification")
@RequiredArgsConstructor
@Traceable
public class EventAssessmentClassificationController {

    private final EventAssessmentClassificationService eventAssessmentClassificationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{eventId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForEvent(
        @PathVariable Integer eventId) {
        return eventAssessmentClassificationService.getAssessmentClassificationsForEvent(eventId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION')")
    @Idempotent
    public EntityAssessmentClassificationResponseDTO createEventAssessmentClassification(
        @RequestBody @Valid EventAssessmentClassificationDTO eventAssessmentClassificationDTO,
        @RequestHeader("Authorization") String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name())) {
            var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));
            eventAssessmentClassificationDTO.setCommissionId(user.getCommission().getId());
        }

        var newEventAssessmentClassification =
            eventAssessmentClassificationService.createEventAssessmentClassification(
                eventAssessmentClassificationDTO);

        return EntityAssessmentClassificationConverter.toDTO(newEventAssessmentClassification);
    }

    @PutMapping("/{entityAssessmentClassificationId}")
    @PreAuthorize("hasAnyAuthority('EDIT_EVENT_ASSESSMENT_CLASSIFICATION', 'EDIT_EVENT_ASSESSMENT_CLASSIFICATION')")
    @EntityClassificationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateEventAssessmentClassification(
        @RequestBody @Valid EventAssessmentClassificationDTO eventAssessmentClassificationDTO,
        @PathVariable Integer entityAssessmentClassificationId) {
        eventAssessmentClassificationService.updateEventAssessmentClassification(
            entityAssessmentClassificationId, eventAssessmentClassificationDTO);
    }
}
