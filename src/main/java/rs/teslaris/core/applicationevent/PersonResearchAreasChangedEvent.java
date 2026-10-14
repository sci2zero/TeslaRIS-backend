package rs.teslaris.core.applicationevent;

import java.util.List;

/**
 * Raised when a person's research areas are set, so modules that keep their own copy can follow.
 * <p>
 * The assessment module stores the same areas as the sub-areas of a person's assessment research
 * area, and that copy is what the points calculation reads. It cannot be written from here, as the
 * assessment module is not a dependency of the one that owns the person, hence the announcement.
 *
 * @param researchAreaIds the person's research areas after the change, possibly empty
 */
public record PersonResearchAreasChangedEvent(
    Integer personId,
    List<Integer> researchAreaIds
) {
}
