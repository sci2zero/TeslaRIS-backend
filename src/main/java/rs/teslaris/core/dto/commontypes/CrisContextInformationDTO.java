package rs.teslaris.core.dto.commontypes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.document.License;

/**
 * A read answers with every configurable identifier, while a write replaces the stored identifier
 * configuration wholesale.
 */
public record CrisContextInformationDTO(
    @NotNull(message = "You have to specify whether assessment should be toggled.")
    Boolean toggleAssessmentModule,

    @NotNull(message = "You have to specify whether digital library should be toggled.")
    Boolean toggleDigitalLibrary,

    @NotNull(message = "You have to specify whether digital repository should be toggled.")
    Boolean toggleDigitalRepository,

    @NotNull(message = "You have to specify whether registration should be toggled.")
    Boolean toggleRegistration,

    @NotBlank(message = "You have to provide a person national ID pattern.")
    String personNationalIdRegularExpression,

    @NotBlank(message = "You have to provide a project national ID pattern.")
    String projectNationalIdRegularExpression,

    @NotBlank(message = "You have to provide an organisation unit national ID pattern.")
    String organisationUnitNationalIdRegularExpression,

    @NotBlank(message = "You have to provide a document national ID pattern.")
    String documentNationalIdRegularExpression,

    @NotNull(message = "You have to specify the metadata license.")
    License metadataLicense,

    @Valid
    Map<ApplicableEntityType, Map<String, IdentifierConfigurationDTO>> identifierConfiguration,

    @Valid
    List<MultilingualContentDTO> nationalIdentifierName,

    // Derived from the ORCID OAuth2 configuration, never persisted, ignored when saving.
    Boolean orcidLoginToggle
) {
}
