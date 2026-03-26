package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.NotNull;

public record PersonFieldVisibilityDTO(
    @NotNull(message = "You have to provide phone number visibility configuration.")
    Boolean phoneNumberVisible,

    @NotNull(message = "You have to provide contact email visibility configuration.")
    Boolean contactEmailVisible,

    @NotNull(message = "You have to provide date of birth visibility configuration.")
    Boolean dateOfBirthVisible,

    @NotNull(message = "You have to provide sex visibility configuration.")
    Boolean sexVisible,

    @NotNull(message = "You have to provide biography visibility configuration.")
    Boolean biographyVisible,

    @NotNull(message = "You have to provide birthplace visibility configuration.")
    Boolean birthplaceVisible
) {
}
