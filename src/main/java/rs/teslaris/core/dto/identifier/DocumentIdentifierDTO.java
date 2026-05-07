package rs.teslaris.core.dto.identifier;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIdentifierDTO extends EntityIdentifierDTO {

    @NotNull(message = "You have to provide document ID.")
    @Positive(message = "Document ID must be a positive number.")
    private Integer documentId;


    public DocumentIdentifierDTO(String value, Integer identifierId, Integer documentId) {
        super(value, identifierId);
        this.documentId = documentId;
    }
}
