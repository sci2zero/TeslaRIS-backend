package rs.teslaris.revisioner.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.dto.FixatorSummaryDTO;
import rs.teslaris.revisioner.service.interfaces.DataQualityFixatorService;

@RestController
@RequestMapping("/api/data-quality-fixator")
@RequiredArgsConstructor
public class DataQualityFixatorController {

    private final DataQualityFixatorService dataQualityFixatorService;


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

    @DeleteMapping("/pipelines/{pipelineName}")
    @PreAuthorize("hasAuthority('MANAGE_DATA_QUALITY_FIXATORS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePipeline(@PathVariable String pipelineName) {
        dataQualityFixatorService.deletePipeline(pipelineName);
    }
}
