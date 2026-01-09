package rs.teslaris.core.dto.commontypes;

import java.util.List;

public record LanguageMigrationDTO(
    String languageCode,
    List<MultilingualContentDTO> name
) {
}
