package rs.teslaris.reporting.controller.leaderboards;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionAssessmentPointsOULeaderboard;
import rs.teslaris.reporting.service.interfaces.OrganisationUnitLeaderboardService;

@RestController
@RequestMapping("/api/leaderboard-data/organisation-unit")
@RequiredArgsConstructor
public class OrganisationUnitLeaderboardController {

    private final OrganisationUnitLeaderboardService organisationUnitLeaderboardService;


    @GetMapping("/publications")
    public List<Pair<OrganisationUnitIndex, Long>> getTopSubUnitsByPublicationCount(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return organisationUnitLeaderboardService.getTopSubUnitsByPublicationCount(institutionId,
            yearFrom, yearTo);
    }

    @GetMapping("/citations")
    public List<Pair<OrganisationUnitIndex, Long>> getSubUnitsWithMostCitations(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return organisationUnitLeaderboardService.getSubUnitsWithMostCitations(institutionId,
            yearFrom, yearTo);
    }

    @GetMapping("/assessment-points")
    public List<CommissionAssessmentPointsOULeaderboard> getSubUnitsWithMostAssessmentPoints(
        @RequestParam Integer institutionId, @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo) {
        return organisationUnitLeaderboardService.getSubUnitsWithMostAssessmentPoints(institutionId,
            yearFrom, yearTo);
    }
}
