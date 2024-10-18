package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.util.jwt.JwtUtil;

@RestController
@RequestMapping("/api/deduplication")
@RequiredArgsConstructor
@Slf4j
public class DeduplicationController {

    private final DeduplicationService deduplicationService;

    private final JwtUtil tokenUtil;

    @GetMapping("/{resultSet}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public Page<DeduplicationSuggestion> fetchDocumentSuggestions(
        @PathVariable EntityType resultSet,
        Pageable pageable) {
        return switch (resultSet) {
            case PUBLICATION ->
                deduplicationService.getDeduplicationSuggestions(pageable, EntityType.PUBLICATION);
            case JOURNAL ->
                deduplicationService.getDeduplicationSuggestions(pageable, EntityType.JOURNAL);
            case BOOK_SERIES ->
                deduplicationService.getDeduplicationSuggestions(pageable, EntityType.BOOK_SERIES);
            case EVENT ->
                deduplicationService.getDeduplicationSuggestions(pageable, EntityType.EVENT);
            case PERSON ->
                deduplicationService.getDeduplicationSuggestions(pageable, EntityType.PERSON);
            case ORGANISATION_UNIT -> deduplicationService.getDeduplicationSuggestions(pageable,
                EntityType.ORGANISATION_UNIT);
            default -> Page.empty();
        };

    }

    @PostMapping("/deduplicate-ahead-of-time")
    @PreAuthorize("hasAuthority('START_DEDUPLICATION_PROCESS')")
    @Idempotent
    public boolean performDeduplicationScan(
        @RequestHeader(value = "Authorization") String bearerToken) {
        log.info("Trying to start deduplication ahead of time.");
        if (!deduplicationService.canPerformDeduplication()) {
            return false;
        }

        deduplicationService.startDeduplicationAsync(tokenUtil.extractUserIdFromToken(bearerToken));

        return true;
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
