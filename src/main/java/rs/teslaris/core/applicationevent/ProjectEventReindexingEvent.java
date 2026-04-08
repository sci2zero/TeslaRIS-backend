package rs.teslaris.core.applicationevent;

import java.util.List;
import rs.teslaris.core.indexmodel.EntityType;

public record ProjectEventReindexingEvent(
    List<EntityType> indexesToRepopulate
) {
}
