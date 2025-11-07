package rs.teslaris.reporting.service.interfaces.leaderboards;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionAssessmentPointsPersonLeaderboard;

@Service
public interface PersonLeaderboardService {

    List<Pair<PersonIndex, Long>> getTopResearchersByPublicationCount(Integer institutionId,
                                                                      Integer fromYear,
                                                                      Integer toYear);

    List<Pair<PersonIndex, Long>> getResearchersWithMostCitations(Integer institutionId,
                                                                  Integer fromYear,
                                                                  Integer toYear);

    List<CommissionAssessmentPointsPersonLeaderboard> getResearchersWithMostAssessmentPoints(
        Integer institutionId,
        Integer fromYear,
        Integer toYear);

    List<Pair<PersonIndex, Long>> getTopResearchersByViewCount(Integer institutionId,
                                                               LocalDate from, LocalDate to);
}
