package rs.teslaris.reporting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.PersonLeaderboardService;

@RestController
@RequestMapping("/api/leaderboard-data/person")
@RequiredArgsConstructor
public class PersonLeaderboardController {

    private final PersonLeaderboardService personLeaderboardService;


    @GetMapping("/publications")
    public List<Pair<PersonIndex, Long>> getTopResearchersByPublicationCount(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return personLeaderboardService.getTopResearchersByPublicationCount(institutionId, yearFrom,
            yearTo);
    }

    @GetMapping("/citations")
    public List<Pair<PersonIndex, Long>> getResearchersWithMostCitations(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return personLeaderboardService.getResearchersWithMostCitations(institutionId, yearFrom,
            yearTo);
    }

    @GetMapping("/assessment-points")
    public List<Pair<PersonIndex, Double>> getResearchersWithMostAssessmentPoints(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return personLeaderboardService.getResearchersWithMostAssessmentPoints(institutionId,
            yearFrom, yearTo);
    }
}
