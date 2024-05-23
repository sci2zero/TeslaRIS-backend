package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequestDTO {

    @NotBlank(message = "Password reset token cannot be blank.")
    private String resetToken;

    @NotBlank(message = "New password cannot be blank.")
    private String newPassword;
}
