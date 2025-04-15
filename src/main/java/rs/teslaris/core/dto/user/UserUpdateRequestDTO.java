package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.user.UserNotificationPeriod;

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

    @NotNull(message = "You must provide a preferred reference language ID.")
    @Positive(message = "Preferred reference languageID must be a positive number.")
    private Integer preferredReferenceLanguageId;

    private Integer organisationalUnitId;

    @NotNull(message = "User notification period cannot be null.")
    private UserNotificationPeriod notificationPeriod;
}
