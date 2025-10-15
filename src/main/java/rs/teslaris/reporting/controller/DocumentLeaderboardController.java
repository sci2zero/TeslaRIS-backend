package rs.teslaris.reporting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.DocumentLeaderboardService;

@RestController
@RequestMapping("/api/leaderboard-data/document")
@RequiredArgsConstructor
public class DocumentLeaderboardController {

    private final DocumentLeaderboardService documentLeaderboardService;

    @GetMapping("/citations")
    public List<Pair<DocumentPublicationIndex, Long>> getPublicationsWithMostCitations(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return documentLeaderboardService.getPublicationsWithMostCitations(institutionId,
            yearFrom, yearTo);
    }
}
