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

    @NotNull(message = "You must provide a preferred UI language tag ID.")
    @Positive(message = "Preferred UI language tag ID must be a positive number.")
    private Integer preferredUILanguageTagId;

    @NotNull(message = "You must provide a preferred reference cataloguing language tag ID.")
    @Positive(message = "Preferred reference cataloguing language tag ID must be a positive number.")
    private Integer preferredReferenceCataloguingLanguageTagId;

    private Integer organisationalUnitId;

    @NotNull(message = "User notification period cannot be null.")
    private UserNotificationPeriod notificationPeriod;
}
