package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

/**
 * A constraint reduced to what a picker needs, which is its name and the fields a picker filters
 * by.
 */
public record ConstraintSummaryDTO(

    String key,

    List<MultilingualContentDTO> title,

    String target,

    QualityDimension dimension,

    String metric
) {
}
