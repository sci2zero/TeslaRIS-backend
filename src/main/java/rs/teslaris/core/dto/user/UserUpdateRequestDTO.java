package rs.teslaris.core.dto.user;

import javax.validation.constraints.Email;
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

    private String firstname;

    private String lastName;

    @NotNull(message = "You must provide a preferred language ID.")
    @Positive(message = "Preferred languageID must be a positive number.")
    private Integer preferredLanguageId;

    private Integer organisationalUnitId;
}
