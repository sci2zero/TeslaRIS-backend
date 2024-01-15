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
public class EmployeeRegistrationRequestDTO {

    @NotBlank(message = "Name cannot be blank.")
    private String name;

    @NotBlank(message = "Surname cannot be blank.")
    private String surname;

    @NotNull(message = "Email cannot be null.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotNull(message = "Admin note cannot be null.")
    private String note;

    @NotNull(message = "You must provide a preferred language ID.")
    @Positive(message = "Preferred language ID must be a positive number.")
    private Integer preferredLanguageId;

    @NotNull(message = "Organisation unit ID cannot be null.")
    @Positive(message = "Organisation Unit ID must be a positive number.")
    private Integer organisationUnitId;
}
