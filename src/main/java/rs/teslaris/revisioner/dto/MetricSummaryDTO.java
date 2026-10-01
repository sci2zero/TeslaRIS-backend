package rs.teslaris.revisioner.dto;

import java.util.List;
import java.util.Set;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public record MetricSummaryDTO(

    String key,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    Set<QualityDimension> dimensions
) {
}
