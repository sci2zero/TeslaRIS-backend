package rs.teslaris.core.dto.commontypes;

import java.time.LocalDate;
import java.util.List;
import rs.teslaris.core.model.commontypes.ApiKeyType;

public record ApiKeyRequest(
    List<MultilingualContentDTO> name,
    ApiKeyType type,
    LocalDate validUntil,
    String clientEmail,
    Integer clientPreferredLanguageId
) {
}
