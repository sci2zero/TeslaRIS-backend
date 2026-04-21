package rs.teslaris.core.dto.identifier;

import java.util.List;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;

public record IdentifierResponseDTO(

    Integer id,

    String code,

    List<MultilingualContentDTO> title,

    List<MultilingualContentDTO> description,

    List<ApplicableEntityType> applicableEntityTypes,

    String regularExpression,

    String uriPrefix
) {
}
