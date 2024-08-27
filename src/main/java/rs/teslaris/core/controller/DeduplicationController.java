package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;

@RestController
@RequestMapping("/api/deduplication")
@RequiredArgsConstructor
public class DeduplicationController {

    private final DeduplicationService deduplicationService;

    @PostMapping("/deduplicate-ahead-of-time")
    @PreAuthorize("hasAuthority('START_DEDUPLICATION_PROCESS')")
    public void performDeduplication() {
        deduplicationService.startDeduplicationProcessBeforeSchedule();
    }
}
