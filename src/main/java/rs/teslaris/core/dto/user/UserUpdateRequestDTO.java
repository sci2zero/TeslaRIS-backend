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
public class UserUpdateRequestDTO {

    @Email(message = "Email must be valid.")
    private String email;

    @NotNull(message = "Old password cannot be null.")
    private String oldPassword;

    @NotNull(message = "New password cannot be null.")
    private String newPassword;

    @NotBlank(message = "First name cannot be blank.")
    private String firstname;

    @NotBlank(message = "Last name cannot be blank.")
    private String lastName;

    @NotNull(message = "You must provide a preferred language ID.")
    @Positive(message = "Preferred languageID must be a positive number.")
    private Integer preferredLanguageId;

    @NotNull(message = "You must provide a person ID.")
    private Integer personId;

    @NotNull(message = "You must provide an authority ID.")
    @Positive(message = "Organisational unit ID must be a positive number.")
    private Integer organisationalUnitId;
}
