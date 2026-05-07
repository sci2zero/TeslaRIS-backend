package rs.teslaris.core.converter.identifier;

import java.util.List;
import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.identifier.IdentifierResponseDTO;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.identifier.Identifier;

public class IdentifierConverter {

    public static IdentifierResponseDTO toDTO(Identifier identifier) {
        return new IdentifierResponseDTO(
            identifier.getId(), identifier.getCode(),
            MultilingualContentConverter.getMultilingualContentDTO(identifier.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(identifier.getDescription()),
            Objects.requireNonNullElse(
                identifier.getApplicableTypes(),
                List.of(ApplicableEntityType.ALL)
            ).stream().toList(),
            identifier.getRegularExpression(),
            identifier.getUriPrefix()
        );
    }
}
