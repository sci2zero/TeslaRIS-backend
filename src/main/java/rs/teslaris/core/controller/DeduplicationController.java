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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.dto.commontypes.DocumentDeduplicationSuggestionDTO;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;

@RestController
@RequestMapping("/api/deduplication")
@RequiredArgsConstructor
public class DeduplicationController {

    private final DeduplicationService deduplicationService;

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public Page<DocumentDeduplicationSuggestionDTO> fetchDocumentSuggestions(Pageable pageable) {
        return deduplicationService.getDeduplicationSuggestions(pageable);
    }

    @PostMapping("/deduplicate-ahead-of-time")
    @PreAuthorize("hasAuthority('START_DEDUPLICATION_PROCESS')")
    @Idempotent
    public boolean performDeduplicationScan() {
        return deduplicationService.startDocumentDeduplicationProcessBeforeSchedule();
    }

    @PatchMapping("/document/{suggestionId}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public void flagDocumentAsNotDuplicate(@PathVariable Integer suggestionId) {
        deduplicationService.flagDocumentAsNotDuplicate(suggestionId);
    }

    @DeleteMapping("/document/{suggestionId}")
    @PreAuthorize("hasAuthority('PERFORM_DEDUPLICATION')")
    public void deleteDocumentSuggestion(@PathVariable Integer suggestionId) {
        deduplicationService.deleteDocumentSuggestion(suggestionId);
    }
}
