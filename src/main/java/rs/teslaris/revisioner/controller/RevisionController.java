package rs.teslaris.revisioner.controller;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.service.interfaces.RevisionService;

@RestController
@RequestMapping("/api/revisions")
@RequiredArgsConstructor
public class RevisionController {

    private final RevisionService revisionService;


    @GetMapping(value = "/{entityType}/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Instant>> getRevisionHistory(@PathVariable String entityType,
                                                            @PathVariable Integer entityId) {
        return ResponseEntity.ok(revisionService.getRevisionTimestamps(entityType, entityId));
    }

    @GetMapping(value = "/{entityType}/{entityId}/at", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getRevisionAtDate(@PathVariable String entityType,
                                                    @PathVariable Integer entityId,
                                                    @RequestParam Instant timestamp) {
        return revisionService
            .getRevisionAtTimestamp(entityType, entityId, timestamp)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/quality-report/{entityType}/{entityId}/at", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MultilingualContentDTO> getQualityReportAtDate(@PathVariable String entityType,
                                                               @PathVariable Integer entityId,
                                                               @RequestParam Instant timestamp) {
        return revisionService.getQualityReportAtTimestamp(entityType, entityId, timestamp);
    }
}
