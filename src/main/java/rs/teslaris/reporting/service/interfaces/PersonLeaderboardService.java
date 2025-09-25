package rs.teslaris.reporting.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;

@Service
public interface PersonLeaderboardService {

    List<Pair<PersonIndex, Long>> getTopResearchersByPublicationCount(Integer institutionId,
                                                                      Integer fromYear,
                                                                      Integer toYear);

    List<Pair<PersonIndex, Long>> getResearchersWithMostCitations(Integer institutionId,
                                                                  Integer fromYear,
                                                                  Integer toYear);

    List<Pair<PersonIndex, Double>> getResearchersWithMostAssessmentPoints(Integer institutionId,
                                                                           Integer fromYear,
                                                                           Integer toYear);
}
