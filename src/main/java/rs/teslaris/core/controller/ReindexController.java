package rs.teslaris.core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.service.interfaces.commontypes.ReindexService;

@RestController
@RequestMapping("/api/reindex")
@RequiredArgsConstructor
public class ReindexController {

    private final ReindexService reindexService;

    @PostMapping
    @PreAuthorize("hasAuthority('REINDEX_DATABASE')")
    @Idempotent
    public void reindexDatabase() {
        reindexService.reindexDatabase();
    }
}
