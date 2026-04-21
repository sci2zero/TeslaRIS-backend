package rs.teslaris.core.dto.identifier;

public record EntityIdentifierResponseDTO(
    Integer id,

    String value,

    IdentifierResponseDTO identifierResponse
) {
}
