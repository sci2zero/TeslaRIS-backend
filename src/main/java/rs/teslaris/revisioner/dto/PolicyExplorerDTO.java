package rs.teslaris.revisioner.dto;

import java.util.List;
import java.util.Map;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

// A whole policy in one response, so the explorer can filter and page without further requests.
public record PolicyExplorerDTO(

    String profileName,

    String version,

    List<PolicyConstraintDTO> constraints,

    Map<QualityDimension, List<MultilingualContentDTO>> dimensionDefinitions
) {
}
