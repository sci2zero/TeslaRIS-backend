package rs.teslaris.importer.dto;

import jakarta.validation.constraints.NotNull;

public record OrganisationUnitImportSourceConfigurationDTO(
    @NotNull(message = "Specify whether Scopus is harvested.")
    Boolean importScopus,

    @NotNull(message = "Specify whether OpenAlex is harvested.")
    Boolean importOpenAlex,

    @NotNull(message = "Specify whether WoS is harvested.")
    Boolean importWebOfScience
) {
}
