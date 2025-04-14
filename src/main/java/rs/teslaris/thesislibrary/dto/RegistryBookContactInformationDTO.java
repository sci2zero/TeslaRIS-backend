package rs.teslaris.thesislibrary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.person.ContactDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistryBookContactInformationDTO {

    @NotNull(message = "You have to provide author's residence country.")
    @Positive
    private Integer residenceCountryId;

    @NotBlank(message = "You have to provide author's street and number.")
    private String streetAndNumber;

    @NotBlank(message = "You have to provide author's place of residence.")
    private String place;

    @NotBlank(message = "You have to provide author's municipality.")
    private String municipality;

    @NotBlank(message = "You have to provide author's postal code.")
    private String postalCode;

    @NotNull(message = "You have to provide author's contact information.")
    @Valid
    private ContactDTO contact;
}
