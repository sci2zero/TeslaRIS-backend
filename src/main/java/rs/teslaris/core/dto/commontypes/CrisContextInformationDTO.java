package rs.teslaris.core.dto.commontypes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import rs.teslaris.core.model.document.License;

public record CrisContextInformationDTO(
    @NotNull(message = "You have to specify whether assessment should be toggled.")
    Boolean toggleAssessmentModule,

    @NotNull(message = "You have to specify whether digital library should be toggled.")
    Boolean toggleDigitalLibrary,

    @NotNull(message = "You have to specify whether digital repository should be toggled.")
    Boolean toggleDigitalRepository,

    @NotBlank(message = "You have to provide a person national ID pattern.")
    String personNationalIdRegularExpression,

    @NotBlank(message = "You have to provide a project national ID pattern.")
    String projectNationalIdRegularExpression,

    @NotBlank(message = "You have to provide an organisation unit national ID pattern.")
    String organisationUnitNationalIdRegularExpression,

    @NotBlank(message = "You have to provide a document national ID pattern.")
    String documentNationalIdRegularExpression,

    @NotNull(message = "You have to specify the metadata license.")
    License metadataLicense
) {
}
