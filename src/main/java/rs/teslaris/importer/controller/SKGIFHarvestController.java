package rs.teslaris.importer.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.RelativeDateDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.commontypes.ScheduledTaskMetadata;
import rs.teslaris.core.model.commontypes.ScheduledTaskType;
import rs.teslaris.core.service.interfaces.commontypes.TaskManagerService;
import rs.teslaris.core.util.exceptionhandling.exception.LoadingException;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.importer.service.interfaces.SKGIFHarvester;
import rs.teslaris.importer.utility.skgif.SKGIFHarvestConfigurationLoader;

@RestController
@RequestMapping("/api/skg-if-harvest")
@RequiredArgsConstructor
public class SKGIFHarvestController {

    private final SKGIFHarvester skgifHarvester;

    private final TaskManagerService taskManagerService;

    private final JwtUtil tokenUtil;


    @GetMapping("/schedule")
    @PreAuthorize("hasAuthority('PERFORM_SKGIF_HARVEST')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleSKGIFHarvest(@RequestParam String sourceName,
                                     @RequestParam RelativeDateDTO from,
                                     @RequestParam RelativeDateDTO until,
                                     @RequestParam LocalDateTime timestamp,
                                     @RequestParam RecurrenceType recurrence,
                                     @RequestHeader("Authorization") String bearerToken) {
        if (!SKGIFHarvestConfigurationLoader.sourceExists(sourceName)) {
            throw new LoadingException(
                "Specified source does not exist in the list of known sources.");
        }

        var userId = tokenUtil.extractUserIdFromToken(bearerToken);
        var taskId = taskManagerService.scheduleTask(
            "SKGIF_Harvest-" + sourceName +
                "-" + from + "_" + until +
                "-" + UUID.randomUUID(), timestamp,
            () -> skgifHarvester.harvest(sourceName, null,
                from.computeDate(), until.computeDate(), userId),
            userId, recurrence);

        taskManagerService.saveTaskMetadata(
            new ScheduledTaskMetadata(taskId, timestamp,
                ScheduledTaskType.SKG_IF_HARVEST, new HashMap<>() {{
                put("sourceName", sourceName);
                put("from", from.toString());
                put("until", until.toString());
                put("userId", userId);
            }}, recurrence));
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAuthority('PERFORM_SKGIF_HARVEST')")
    public List<String> getSources() {
        return skgifHarvester.getSources();
    }
}
