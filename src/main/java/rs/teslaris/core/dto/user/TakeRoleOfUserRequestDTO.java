package rs.teslaris.core.dto.user;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TakeRoleOfUserRequestDTO {

    @Email(message = "Email must be valid.")
    private String userEmail;
}
