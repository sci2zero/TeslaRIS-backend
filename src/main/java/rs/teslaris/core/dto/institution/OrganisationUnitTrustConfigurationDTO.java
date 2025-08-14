package rs.teslaris.core.dto.institution;

import jakarta.validation.constraints.NotNull;

public record OrganisationUnitTrustConfigurationDTO(
    @NotNull(message = "You have to specify whether you trust new publications.")
    Boolean trustNewPublications,

    @NotNull(message = "You have to specify whether you trust new document files.")
    Boolean trustNewDocumentFiles
) {
}
