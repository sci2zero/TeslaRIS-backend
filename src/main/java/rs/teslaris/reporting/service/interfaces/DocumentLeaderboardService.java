package rs.teslaris.reporting.service.interfaces;

import java.util.List;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.util.functional.Pair;

public interface DocumentLeaderboardService {

    List<Pair<DocumentPublicationIndex, Long>> getPublicationsWithMostCitations(
        Integer institutionId,
        Integer fromYear,
        Integer toYear);
}
