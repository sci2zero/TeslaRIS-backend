package rs.teslaris.revisioner.controller;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;

@RestController
@RequestMapping("/api/quality-assessment-backfill")
@RequiredArgsConstructor
@Traceable
public class QualityAssessmentBackfillController {

    private final JwtUtil tokenUtil;

    private final QualityAssessmentBackfillService qualityAssessmentBackfillService;


    @PostMapping("/schedule")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_TASK', 'BACKFILL_ENTITY_REVISION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleQualityAssessmentBackfill(
        @RequestHeader("Authorization") String bearerToken,
        @RequestParam("entityTypes") List<QualityAssessmentTarget> entityTypes,
        @RequestParam(value = "personIds", required = false) List<Integer> personIds,
        @RequestParam(value = "organisationUnitIds", required = false)
        List<Integer> organisationUnitIds,
        @RequestParam("profileName") String profileName,
        @RequestParam(value = "rewriteExistingAssessments", defaultValue = "false")
        Boolean rewriteExistingAssessments,
        @RequestParam("timestamp") Instant timestamp,
        @RequestParam("recurrence") RecurrenceType recurrenceType) {
        qualityAssessmentBackfillService.scheduleBackfill(timestamp, entityTypes, personIds,
            organisationUnitIds, profileName, rewriteExistingAssessments,
            tokenUtil.extractUserIdFromToken(bearerToken), recurrenceType);
    }
}
