package rs.teslaris.core.dto.institution;

import jakarta.validation.constraints.NotNull;

public record OrganisationUnitOutputConfigurationDTO(
    @NotNull(message = "You have to specify whether you want to show outputs.")
    Boolean showOutputs,

    @NotNull(message = "You have to specify whether you are searching by specified affiliations.")
    Boolean showBySpecifiedAffiliation,

    @NotNull(message = "You have to specify whether you are searching by publication year employments.")
    Boolean showByPublicationYearEmployments,

    @NotNull(message = "You have to specify whether you are searching by current employments.")
    Boolean showByCurrentEmployments
) {
}
