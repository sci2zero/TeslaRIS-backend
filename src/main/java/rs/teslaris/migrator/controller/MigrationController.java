package rs.teslaris.migrator.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.util.jwt.JwtUtil;
import rs.teslaris.migrator.dto.MigrationFailureDTO;
import rs.teslaris.migrator.dto.MigrationRequestDTO;
import rs.teslaris.migrator.dto.MigrationRunResponseDTO;
import rs.teslaris.migrator.service.interfaces.MigrationService;
import rs.teslaris.migrator.util.MigrationEntityType;

@RestController
@RequestMapping("/api/migrator")
@RequiredArgsConstructor
public class MigrationController {

    private final MigrationService migrationService;

    private final JwtUtil tokenUtil;


    @PostMapping("/run")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String startRun(@RequestParam("source") String source,
                           @RequestParam("entityType") MigrationEntityType entityType,
                           @RequestParam(value = "batchSize", required = false) Integer batchSize,
                           @RequestParam(value = "performIndex", defaultValue = "false")
                           Boolean performIndex,
                           @RequestParam(value = "resume", defaultValue = "false") Boolean resume,
                           @RequestParam(value = "modifiedAfter", required = false)
                           String modifiedAfter,
                           @RequestHeader("Authorization") String bearerToken) {
        return migrationService.startRun(new MigrationRequestDTO(
            source, entityType, batchSize, performIndex, resume, modifiedAfter,
            tokenUtil.extractUserIdFromToken(bearerToken)));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    public List<MigrationRunResponseDTO> listRuns(
        @RequestParam(value = "source", required = false) String source,
        @RequestParam(value = "entityType", required = false) MigrationEntityType entityType,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size) {
        return migrationService.listRuns(source, entityType, PageRequest.of(page, size));
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    public MigrationRunResponseDTO getRun(@PathVariable String runId) {
        return migrationService.getRun(runId);
    }

    @GetMapping("/runs/{runId}/failures")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    public List<MigrationFailureDTO> getFailures(@PathVariable String runId,
                                                 @RequestParam(value = "page", defaultValue = "0")
                                                 int page,
                                                 @RequestParam(value = "size", defaultValue = "50")
                                                 int size) {
        return migrationService.getFailures(runId, PageRequest.of(page, size));
    }

    @PostMapping("/runs/{runId}/retry-failed")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public String retryFailed(@PathVariable String runId,
                              @RequestHeader("Authorization") String bearerToken) {
        return migrationService.retryFailed(runId, tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @GetMapping("/pipelines")
    @PreAuthorize("hasAuthority('PERFORM_MIGRATION')")
    public List<String> listPipelines() {
        return migrationService.listRegisteredPipelines();
    }
}
