package rs.teslaris.revisioner.controller;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.dto.FixatorSummaryDTO;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorRunService;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorService;

@RestController
@RequestMapping("/api/data-quality-fixator")
@RequiredArgsConstructor
public class DataQualityFixatorController {

    private final DataQualityFixatorService dataQualityFixatorService;

    private final DataQualityFixatorRunService dataQualityFixatorRunService;

    private final JwtUtil tokenUtil;


    @GetMapping(value = "/fixators", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    public List<FixatorSummaryDTO> listFixators() {
        return dataQualityFixatorService.listFixators();
    }

    @GetMapping(value = "/pipelines", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    public List<DataQualityFixatorPipelineDTO> listPipelines() {
        return dataQualityFixatorService.listPipelines();
    }

    @PostMapping(value = "/pipelines", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    @ResponseStatus(HttpStatus.CREATED)
    public DataQualityFixatorPipelineDTO createPipeline(
        @RequestBody @Valid DataQualityFixatorPipelineDTO pipeline) {
        return dataQualityFixatorService.createPipeline(pipeline);
    }

    @PutMapping(value = "/pipelines/{pipelineName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    public DataQualityFixatorPipelineDTO updatePipeline(@PathVariable String pipelineName,
                                                        @RequestBody @Valid
                                                        DataQualityFixatorPipelineDTO pipeline) {
        return dataQualityFixatorService.updatePipeline(pipelineName, pipeline);
    }

    @PostMapping("/pipelines/{pipelineName}/schedule")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_TASK', 'MANAGE_DATA_QUALITY_FIXATORS')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleFixatorRun(@PathVariable String pipelineName,
                                   @RequestParam("timestamp") Instant timestamp,
                                   @RequestParam("recurrence") RecurrenceType recurrence,
                                   @RequestHeader("Authorization") String bearerToken) {
        dataQualityFixatorService.assertPipelineExists(pipelineName);
        dataQualityFixatorRunService.scheduleFixatorRun(timestamp, pipelineName,
            tokenUtil.extractUserIdFromToken(bearerToken), recurrence);
    }

    @PostMapping("/pipelines/schedule")
    @PreAuthorize("hasAnyAuthority('SCHEDULE_TASK', 'MANAGE_DATA_QUALITY_FIXATORS')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void scheduleAllFixatorRuns(@RequestParam("timestamp") Instant timestamp,
                                       @RequestParam("recurrence") RecurrenceType recurrence,
                                       @RequestHeader("Authorization") String bearerToken) {
        dataQualityFixatorRunService.scheduleFixatorRun(timestamp, null,
            tokenUtil.extractUserIdFromToken(bearerToken), recurrence);
    }

    @DeleteMapping("/pipelines/{pipelineName}")
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePipeline(@PathVariable String pipelineName) {
        dataQualityFixatorService.deletePipeline(pipelineName);
    }
}
