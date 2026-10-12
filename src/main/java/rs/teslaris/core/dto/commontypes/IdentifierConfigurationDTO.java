package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotNull;

public record IdentifierConfigurationDTO(

    @NotNull(message = "You have to specify whether the identifier is hidden when not set.")
    Boolean hideWhenNotSet,

    @NotNull(message = "You have to specify whether the identifier is featured.")
    Boolean featured,

    @NotNull(message = "You have to specify whether the identifier can be updated.")
    Boolean canBeUpdated
) {
}
