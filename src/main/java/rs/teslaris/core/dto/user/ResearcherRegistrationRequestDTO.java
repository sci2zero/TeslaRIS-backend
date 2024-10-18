package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @NotNull(message = "You have to provide first name, leave it blank if you don't want to provide a value.")
    private String firstName;

    @NotNull(message = "You have to provide last name, leave it blank if you don't want to provide a value.")
    private String lastName;

    @Positive(message = "Organisation Unit ID must be a positive number.")
    private Integer organisationUnitId;
}
