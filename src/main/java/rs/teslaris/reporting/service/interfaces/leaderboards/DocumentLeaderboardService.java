package rs.teslaris.reporting.service.interfaces.leaderboards;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.core.util.functional.Pair;

public interface DocumentLeaderboardService {

    List<Pair<DocumentPublicationIndex, Long>> getPublicationsWithMostCitations(
        Integer institutionId,
        Integer fromYear,
        Integer toYear);

    List<Pair<DocumentPublicationIndex, Long>> getTopPublicationsByStatisticCount(
        Integer institutionId,
        StatisticsType statisticsType,
        LocalDate from,
        LocalDate to,
        Boolean onlyTheses,
        List<ThesisType> allowedThesisTypes);

    List<Integer> getEligibleDocumentIds(Integer institutionId, Boolean onlyTheses,
                                         List<ThesisType> allowedThesisTypes);
}
