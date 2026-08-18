package rs.teslaris.revisioner.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.model.QualityAssessmentTarget;
import rs.teslaris.revisioner.service.interfaces.QualityAssessmentBackfillService;

@RestController
@RequestMapping("/api/quality-assessment-backfill")
@RequiredArgsConstructor
@Traceable
public class QualityAssessmentBackfillController {

    private final JwtUtil tokenUtil;

    private final TaskManagerService taskManagerService;

    private final QualityAssessmentBackfillService qualityAssessmentBackfillService;


    @PostMapping("/schedule")
    @PreAuthorize("hasAuthority('SCHEDULE_TASK')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleQualityAssessmentBackfill(
        @RequestHeader("Authorization") String bearerToken,
        @RequestParam("entityTypes") List<QualityAssessmentTarget> entityTypes,
        @RequestParam(value = "personIds", required = false) List<Integer> personIds,
        @RequestParam(value = "organisationUnitIds", required = false)
        List<Integer> organisationUnitIds,
        @RequestParam(value = "rewriteExistingAssessments", defaultValue = "false")
        Boolean rewriteExistingAssessments,
        @RequestParam("timestamp") LocalDateTime timestamp,
        @RequestParam("recurrence") RecurrenceType recurrenceType) {
        var userId = tokenUtil.extractUserIdFromToken(bearerToken);

        var taskId = taskManagerService.scheduleTask(
            "Quality_Assessment_Backfill-" +
                entityTypes.stream().map(Enum::name).collect(Collectors.joining("_")) +
                "-" + UUID.randomUUID(), timestamp,
            () -> qualityAssessmentBackfillService.performBackfill(entityTypes, personIds,
                organisationUnitIds, rewriteExistingAssessments),
            userId, recurrenceType);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.QUALITY_ASSESSMENT_BACKFILL, new HashMap<>() {{
                put("entityTypes", entityTypes.stream().map(Enum::name).toList());
                put("personIds", personIds);
                put("organisationUnitIds", organisationUnitIds);
                put("rewriteExistingAssessments", rewriteExistingAssessments);
                put("userId", userId);
            }}, recurrenceType));
    }
}
