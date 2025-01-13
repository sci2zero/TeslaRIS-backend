package rs.teslaris.core.assessment.controller;

import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.assessment.converter.EntityAssessmentClassificationConverter;
import rs.teslaris.core.assessment.dto.EntityAssessmentClassificationResponseDTO;
import rs.teslaris.core.assessment.dto.PublicationSeriesAssessmentClassificationDTO;
import rs.teslaris.core.assessment.service.interfaces.PublicationSeriesAssessmentClassificationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/assessment/publication-series-assessment-classification")
@RequiredArgsConstructor
public class PublicationSeriesAssessmentClassificationController {

    private final PublicationSeriesAssessmentClassificationService
        publicationSeriesAssessmentClassificationService;

    private final JwtUtil tokenUtil;


    @GetMapping("/{publicationSeriesId}")
    public List<EntityAssessmentClassificationResponseDTO> getAssessmentClassificationsForPublicationSeries(
        @PathVariable Integer publicationSeriesId) {
        return publicationSeriesAssessmentClassificationService.getAssessmentClassificationsForPublicationSeries(
            publicationSeriesId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION')")
    @Idempotent
    public EntityAssessmentClassificationResponseDTO createPublicationSeriesAssessmentClassification(
        @RequestBody
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO) {
        var newPublicationSeriesAssessmentClassification =
            publicationSeriesAssessmentClassificationService.createPublicationSeriesAssessmentClassification(
                publicationSeriesAssessmentClassificationDTO);

        return EntityAssessmentClassificationConverter.toDTO(
            newPublicationSeriesAssessmentClassification);
    }

    @PutMapping("/{publicationSeriesAssessmentClassificationId}")
    @PreAuthorize("hasAuthority('EDIT_ENTITY_ASSESSMENT_CLASSIFICATION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePublicationSeriesAssessmentClassification(
        @RequestBody
        PublicationSeriesAssessmentClassificationDTO publicationSeriesAssessmentClassificationDTO,
        @PathVariable Integer publicationSeriesAssessmentClassificationId) {
        publicationSeriesAssessmentClassificationService.updatePublicationSeriesAssessmentClassification(
            publicationSeriesAssessmentClassificationId,
            publicationSeriesAssessmentClassificationDTO);
    }

    @PostMapping("/schedule-classification")
    @Idempotent
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    public void createPublicationSeriesAssessmentClassification(
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("commissionId") Integer commissionId,
        @RequestParam("classificationYears") List<Integer> classificationYears,
        @RequestHeader("Authorization") String bearerToken) {
        publicationSeriesAssessmentClassificationService.scheduleClassification(timestamp,
            commissionId, tokenUtil.extractUserIdFromToken(bearerToken), classificationYears);
    }
}