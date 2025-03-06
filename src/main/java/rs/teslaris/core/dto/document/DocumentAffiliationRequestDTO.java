package rs.teslaris.core.dto.document;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DocumentAffiliationRequestDTO(
    @NotNull(message = "Document IDs must not be null.")
    List<Integer> documentIds,

    Boolean deleteOthers
) {
}
