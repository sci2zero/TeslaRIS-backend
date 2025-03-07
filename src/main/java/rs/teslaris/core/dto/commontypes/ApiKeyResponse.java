package rs.teslaris.core.dto.commontypes;

import java.util.List;
import rs.teslaris.core.model.commontypes.ApiKeyType;

public record ApiKeyResponse(
    List<MultilingualContentDTO> name,
    String value,
    ApiKeyType type
) {
}
