package rs.teslaris.reporting.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.util.functional.Pair;

public record CommissionAssessmentPointsPersonLeaderboard(
    Integer commissionId,
    List<MultilingualContentDTO> commissionDescription,
    List<Pair<PersonIndex, Double>> leaderboardData
) {
}
