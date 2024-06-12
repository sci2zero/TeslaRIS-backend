package rs.teslaris.core.dto.person;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactDTO {

    @Email(message = "Email must be valid.")
    private String contactEmail;

    private String phoneNumber;
}
