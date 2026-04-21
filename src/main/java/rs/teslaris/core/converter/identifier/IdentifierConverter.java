package rs.teslaris.core.converter.identifier;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.model.identifier.Identifier;

public class IdentifierConverter {

    public static IdentifierResponseDTO toDTO(Identifier identifier) {
        return new IdentifierResponseDTO(
            identifier.getId(), identifier.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(identifier.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(identifier.getDescription()),
            identifier.getApplicableTypes().stream().toList(), identifier.getRegularExpression(),
            identifier.getUriPrefix());
    }
}
