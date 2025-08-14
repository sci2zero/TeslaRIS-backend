package rs.teslaris.core.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.commontypes.ReindexRequestDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/reindex")
@RequiredArgsConstructor
@Traceable
public class ReindexController {

    private final ReindexService reindexService;

    private final TaskManagerService taskManagerService;

    private final JwtUtil tokenUtil;


    @PostMapping("/schedule")
    @PreAuthorize("hasAnyAuthority('REINDEX_DATABASE', 'SCHEDULE_TASK')")
    @Idempotent
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleDatabaseReindex(@RequestParam("timestamp")
                                        LocalDateTime timestamp,
                                        @RequestParam(defaultValue = "ONCE")
                                        RecurrenceType recurrence,
                                        @RequestHeader("Authorization")
                                        String bearerToken,
                                        @RequestBody ReindexRequestDTO reindexRequest) {
        var taskId = taskManagerService.scheduleTask(
            "DatabaseReindex-" +
                StringUtils.join(reindexRequest.getIndexesToRepopulate().stream().map(
                    EntityType::name).toList(), "-") +
                "-" + UUID.randomUUID(),
            timestamp,
            () -> reindexService.reindexDatabase(reindexRequest.getIndexesToRepopulate()),
            tokenUtil.extractUserIdFromToken(bearerToken), recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.REINDEXING, new HashMap<>() {{
                put("indexesToRepopulate", reindexRequest.getIndexesToRepopulate());
                put("userId", tokenUtil.extractUserIdFromToken(bearerToken));
            }}, recurrence));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REINDEX_DATABASE')")
    @Idempotent
    public void reindexDatabase(@RequestBody ReindexRequestDTO reindexRequest) {
        reindexService.reindexDatabase(reindexRequest.getIndexesToRepopulate());
    }
}
