package rs.teslaris.reporting.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.util.functional.Pair;

public record CommissionAssessmentPointsOULeaderboard(
    Integer commissionId,
    List<MultilingualContentDTO> commissionDescription,
    List<Pair<OrganisationUnitIndex, Double>> leaderboardData
) {
}
