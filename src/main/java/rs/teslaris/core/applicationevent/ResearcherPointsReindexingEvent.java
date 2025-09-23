package rs.teslaris.core.applicationevent;

import java.util.List;

public record ResearcherPointsReindexingEvent(
    List<Integer> personIds
) {
}
