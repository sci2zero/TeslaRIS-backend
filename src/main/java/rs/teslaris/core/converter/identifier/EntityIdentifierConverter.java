package rs.teslaris.core.converter.identifier;

import rs.teslaris.core.dto.identifier.EntityIdentifierResponseDTO;
import rs.teslaris.core.model.identifier.EntityIdentifier;

public class EntityIdentifierConverter {

    public static EntityIdentifierResponseDTO toDTO(EntityIdentifier entityIdentifier) {
        return new EntityIdentifierResponseDTO(
            entityIdentifier.getId(),
            entityIdentifier.getValue(),
            IdentifierConverter.toDTO(entityIdentifier.getIdentifier()));
    }
}
