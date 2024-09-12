package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.indexmodel.IndexType;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/deduplication")
@RequiredArgsConstructor
public class DeduplicationController {

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;

    @GetMapping("/{resultSet}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public Page<DeduplicationSuggestion> fetchDocumentSuggestions(@PathVariable IndexType resultSet,
                                                                  Pageable pageable) {
        return switch (resultSet) {
            case PUBLICATION ->
                deduplicationService.getDeduplicationSuggestions(pageable, IndexType.PUBLICATION);
            case JOURNAL ->
                deduplicationService.getDeduplicationSuggestions(pageable, IndexType.JOURNAL);
            case EVENT ->
                deduplicationService.getDeduplicationSuggestions(pageable, IndexType.EVENT);
            case PERSON ->
                deduplicationService.getDeduplicationSuggestions(pageable, IndexType.PERSON);
            default -> null;
        };

    }

    @PostMapping("/deduplicate-ahead-of-time")
    @PreAuthorize("hasAuthority('START_DEDUPLICATION_PROCESS')")
    @Idempotent
    public boolean performDeduplicationScan(
        @RequestHeader(value = "Authorization") String bearerToken) {
        return deduplicationService.startDeduplicationProcessBeforeSchedule(
            tokenUtil.extractUserIdFromToken(bearerToken));
    }

    @PatchMapping("/suggestion/{suggestionId}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public void flagAsNotDuplicate(@PathVariable String suggestionId) {
        deduplicationService.flagAsNotDuplicate(suggestionId);
    }

    @DeleteMapping("/suggestion/{suggestionId}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public void deleteDeduplicationSuggestion(@PathVariable String suggestionId) {
        deduplicationService.deleteSuggestion(suggestionId);
    }
}
