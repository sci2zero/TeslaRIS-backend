package rs.teslaris.reporting.controller.leaderboards;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.leaderboards.DocumentLeaderboardService;

@RestController
@RequestMapping("/api/leaderboard-data/digital-library")
@RequiredArgsConstructor
public class DigitalLibraryLeaderboardController {

    private final DocumentLeaderboardService documentLeaderboardService;


    @GetMapping("/statistics")
    public List<Pair<DocumentPublicationIndex, Long>> getThesesWithMostStatisticsCount(
        @RequestParam Integer institutionId, @RequestParam LocalDate from,
        @RequestParam LocalDate to, @RequestParam StatisticsType statisticsType,
        @RequestParam(required = false) List<ThesisType> allowedThesisTypes) {
        return documentLeaderboardService.getTopPublicationsByStatisticCount(institutionId,
            statisticsType, from, to, true, allowedThesisTypes);
    }
}
