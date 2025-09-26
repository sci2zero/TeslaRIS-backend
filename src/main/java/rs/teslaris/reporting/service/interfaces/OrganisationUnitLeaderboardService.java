package rs.teslaris.reporting.service.interfaces;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionAssessmentPointsOULeaderboard;

@Service
public interface OrganisationUnitLeaderboardService {

    List<Pair<OrganisationUnitIndex, Long>> getTopSubUnitsByPublicationCount(Integer institutionId,
                                                                             Integer fromYear,
                                                                             Integer toYear);

    List<Pair<OrganisationUnitIndex, Long>> getSubUnitsWithMostCitations(Integer institutionId,
                                                                         Integer fromYear,
                                                                         Integer toYear);

    List<CommissionAssessmentPointsOULeaderboard> getSubUnitsWithMostAssessmentPoints(
        Integer institutionId,
        Integer fromYear,
        Integer toYear);
}
