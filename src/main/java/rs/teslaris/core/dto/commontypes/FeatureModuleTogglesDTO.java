package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotNull;

public record FeatureModuleTogglesDTO(
    @NotNull(message = "You have to specify whether assessment should be toggled.")
    Boolean toggleAssessmentModule,

    @NotNull(message = "You have to specify whether digital library should be toggled.")
    Boolean toggleDigitalLibrary,

    @NotNull(message = "You have to specify whether digital repository should be toggled.")
    Boolean toggleDigitalRepository
) {
}
