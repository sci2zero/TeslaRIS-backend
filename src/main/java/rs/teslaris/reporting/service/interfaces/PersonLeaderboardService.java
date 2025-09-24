package rs.teslaris.reporting.service.interfaces;

import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.PersonIndex;

@Service
public interface PersonLeaderboardService {

    Map<PersonIndex, Long> getTopResearchersByPublicationCount(Integer institutionId,
                                                               Integer fromYear,
                                                               Integer toYear) throws IOException;

    Map<PersonIndex, Long> getResearchersWithMostCitations(Integer institutionId, Integer fromYear,
                                                           Integer toYear);

    Map<PersonIndex, Long> getResearchersWithMostAssessmentPoints(Integer institutionId,
                                                                  Integer fromYear, Integer toYear);
}
