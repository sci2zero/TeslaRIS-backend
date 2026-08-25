package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

/**
 * A constraint reduced to what a picker needs - its key and its name.
 */
public record ConstraintSummaryDTO(

    String key,

    List<MultilingualContentDTO> title
) {
}
