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
public class ResearcherRegistrationRequestDTO {

    @Email(message = "Email must be valid.")
    private String email;

    @NotBlank(message = "Password cannot be blank.")
    private String password;

    @NotNull(message = "You must provide a preferred language ID.")
    @Positive(message = "Preferred languageID must be a positive number.")
    private Integer preferredLanguageId;

    @Positive(message = "Person ID must be a positive number.")
    private Integer personId;

    private String firstName;
    private String lastName;

    @Positive(message = "Organisation Unit ID must be a positive number.")
    private Integer organisationUnitId;
}
