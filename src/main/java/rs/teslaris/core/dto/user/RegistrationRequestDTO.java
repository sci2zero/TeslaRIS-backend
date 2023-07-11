package rs.teslaris.core.dto.user;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {

    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Password cannot be blank.")
    private String password;

    @NotBlank(message = "First name cannot be blank.")
    private String firstname;

    @NotBlank(message = "Last name cannot be blank.")
    private String lastName;

    @NotNull(message = "You must provide a preferred language ID.")
    @Positive(message = "Preferred languageID must be a positive number.")
    private Integer preferredLanguageId;

    @NotNull(message = "You must provide an authority ID.")
    @Positive(message = "Authority ID must be a positive number.")
    private Integer authorityId;

    @NotNull(message = "You must provide a person ID.")
    private Integer personId;

    @NotNull(message = "You must provide an authority ID.")
    @Positive(message = "Organisational unit ID must be a positive number.")
    private Integer organisationalUnitId;
}
