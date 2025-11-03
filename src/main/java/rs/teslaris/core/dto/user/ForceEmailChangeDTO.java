package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.Email;

public record ForceEmailChangeDTO(

    @Email(message = "Invalid email format.")
    String newEmail
) {
}
