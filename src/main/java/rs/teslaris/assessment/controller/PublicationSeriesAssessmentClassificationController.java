package rs.teslaris.assessment.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.assessment.annotation.EntityClassificationEditCheck;
import rs.teslaris.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.assessment.model.EntityClassificationSource;
import rs.teslaris.assessment.service.interfaces.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.user.UserRole;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/publication-series-assessment-classification")
@RequiredArgsConstructor
@Traceable
public class PublicationSeriesAssessmentClassificationController {

    private final PublicationSeriesAssessmentClassificationService
        publicationSeriesAssessmentClassificationService;

    private final JwtUtil tokenUtil;

    private final UserService userService;


    @GetMapping("/{publicationSeriesId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        @PathVariable Integer publicationSeriesId) {
        return publicationSeriesAssessmentClassificationService.getAssessmentClassificationsForPublicationSeries(
            publicationSeriesId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION', 'EDIT_PUB_SERIES_ASSESSMENT_CLASSIFICATION')")
    @Idempotent
    public EntityAssessmentClassificationResponseDTO createPublicationSeriesAssessmentClassification(
        @RequestBody
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO,
        @RequestHeader("Authorization") String bearerToken) {
        if (tokenUtil.extractUserRoleFromToken(bearerToken).equals(UserRole.COMMISSION.name())) {
            var user = userService.findOne(tokenUtil.extractUserIdFromToken(bearerToken));
            publicationSeriesAssessmentClassificationDTO.setCommissionId(
                user.getCommission().getId());
        }

        var newPublicationSeriesAssessmentClassification =
            publicationSeriesAssessmentClassificationService.createPublicationSeriesAssessmentClassification(
                publicationSeriesAssessmentClassificationDTO);

        return EntityAssessmentClassificationConverter.toDTO(
            newPublicationSeriesAssessmentClassification);
    }

    @PutMapping("/{entityAssessmentClassificationId}")
    @PreAuthorize("hasAnyAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION', 'EDIT_PUB_SERIES_ASSESSMENT_CLASSIFICATION')")
    @EntityClassificationEditCheck
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePublicationSeriesAssessmentClassification(
        @RequestBody
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO,
        @PathVariable Integer entityAssessmentClassificationId) {
        publicationSeriesAssessmentClassificationService.updatePublicationSeriesAssessmentClassification(
            entityAssessmentClassificationId,
            publicationSeriesAssessmentClassificationDTO);
    }

    @PostMapping("/schedule-classification")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void schedulePublicationSeriesAssessmentClassificationComputation(
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("commissionId") Integer commissionId,
        @RequestParam("classificationYears") List<Integer> classificationYears,
        @RequestParam(value = "journalIds", required = false) List<Integer> journalIds,
        @RequestHeader("Authorization") String bearerToken) {
        publicationSeriesAssessmentClassificationService.scheduleClassification(timestamp,
            commissionId, tokenUtil.extractUserIdFromToken(bearerToken), classificationYears,
            Objects.nonNull(journalIds) ? journalIds : List.of());
    }

    @PostMapping("/schedule-classification-load")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void schedulePublicationSeriesAssessmentClassificationLoad(
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("source") EntityClassificationSource source,
        @RequestParam("commissionId") Integer commissionId,
        @RequestHeader("Authorization") String bearerToken) {
        publicationSeriesAssessmentClassificationService.scheduleClassificationLoading(timestamp,
            source, tokenUtil.extractUserIdFromToken(bearerToken), commissionId);
    }
}