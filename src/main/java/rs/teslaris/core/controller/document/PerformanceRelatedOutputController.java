package rs.teslaris.core.controller.document;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.PublicationEditCheck;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.util.signposting.FairSignpostingL1Utility;

@RestController
@RequestMapping("api/performance-related-output")
@RequiredArgsConstructor
@Traceable
public class PerformanceRelatedOutputController {

    private final PerformanceRelatedOutputService performanceRelatedOutputService;

    @GetMapping("/{documentId}")
    public ResponseEntity<PerformanceRelatedOutputDTO> readPerformanceRelatedOutput(
        @PathVariable Integer documentId) {
        var dto = performanceRelatedOutputService.readPerformanceRelatedOutputById(documentId);

        return ResponseEntity.ok()
            .headers(
                FairSignpostingL1Utility.constructHeaders(dto, "/api/performanceRelatedOutput"))
            .body(dto);
    }

    @GetMapping("/old-id/{oldId}")
    public PerformanceRelatedOutputDTO readPerformanceRelatedOutputByOldId(
        @PathVariable Integer oldId) {
        return performanceRelatedOutputService.readPerformanceRelatedOutputByOldId(oldId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PublicationEditCheck("CREATE")
    @Idempotent
    public PerformanceRelatedOutputDTO createPerformanceRelatedOutput(
        @RequestBody @Valid PerformanceRelatedOutputDTO performanceRelatedOutput) {
        var savedPerformanceRelatedOutput =
            performanceRelatedOutputService.createPerformanceRelatedOutput(performanceRelatedOutput,
                true);

        performanceRelatedOutput.setId(savedPerformanceRelatedOutput.getId());
        return performanceRelatedOutput;
    }

    @PutMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void editPerformanceRelatedOutput(@PathVariable Integer documentId,
                                             @RequestBody @Valid
                                             PerformanceRelatedOutputDTO performanceRelatedOutput) {
        performanceRelatedOutputService.editPerformanceRelatedOutput(documentId,
            performanceRelatedOutput);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PublicationEditCheck
    public void deletePerformanceRelatedOutput(@PathVariable Integer documentId) {
        performanceRelatedOutputService.deletePerformanceRelatedOutput(documentId);
    }
}
