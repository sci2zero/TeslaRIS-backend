package rs.teslaris.reporting.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.service.interfaces.GlobalLeaderboardService;

@RestController
@RequestMapping("/api/leaderboard-data/global")
@RequiredArgsConstructor
public class GlobalLeaderboardController {

    private final GlobalLeaderboardService globalLeaderboardService;

    @GetMapping("/person-citations")
    public List<Pair<PersonIndex, Long>> getPersonsWithMostCitations() {
        return globalLeaderboardService.getPersonsWithMostCitations();
    }

    @GetMapping("/organisation-unit-citations")
    public List<Pair<OrganisationUnitIndex, Long>> getInstitutionsWithMostCitations() {
        return globalLeaderboardService.getInstitutionsWithMostCitations();
    }

    @GetMapping("/document-citations")
    public List<Pair<DocumentPublicationIndex, Long>> getDocumentsWithMostCitations() {
        return globalLeaderboardService.getDocumentsWithMostCitations();
    }
}
