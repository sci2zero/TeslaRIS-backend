package rs.teslaris.revisioner.controller;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.revisioner.dto.RevisionDTO;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

@RestController
@RequestMapping("/api/revision")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;


    @GetMapping(value = "/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<List<RevisionDTO>> getRevisionHistory(@PathVariable String entityType,
                                                                @PathVariable Integer entityId) {
        return ResponseEntity.ok(revisionService.getRevisions(entityType, entityId));
    }

    @GetMapping(value = "/{entityType}/{entityId}/at", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('ASSESS_DATA_QUALITY')")
    public ResponseEntity<String> getRevisionAtDate(@PathVariable String entityType,
                                                    @PathVariable Integer entityId,
                                                    @RequestParam Instant timestamp) {
        return revisionService
            .getRevisionAtTimestamp(entityType, entityId, timestamp)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{entityType}/{entityId}/restore/{majorVersion}/{minorVersion}")
    @PreAuthorize("hasAuthority('RESTORE_ENTITY_REVISION')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restoreRevision(@PathVariable String entityType,
                                @PathVariable Integer entityId,
                                @PathVariable Integer majorVersion,
                                @PathVariable Integer minorVersion) {
        revisionService.restoreRevision(entityType, entityId, majorVersion, minorVersion);
    }
}
