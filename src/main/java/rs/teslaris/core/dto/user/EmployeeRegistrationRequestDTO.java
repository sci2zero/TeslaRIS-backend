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
public class EmployeeRegistrationRequestDTO {

    @NotBlank(message = "Name cannot be blank.")
    private String name;

    @NotNull(message = "You have to provide surname, leave it blank if you don't want to provide a value.")
    private String surname;

    @NotNull(message = "Email cannot be null.")
    @Email(message = "Email must be valid.")
    private String email;

    @NotNull(message = "Admin note cannot be null.")
    private String note;

    @NotNull(message = "You must provide a preferred language tag ID.")
    @Positive(message = "Preferred language tag ID must be a positive number.")
    private Integer preferredLanguageId;

    @NotNull(message = "Organisation unit ID cannot be null.")
    @Positive(message = "Organisation Unit ID must be a positive number.")
    private Integer organisationUnitId;
}
