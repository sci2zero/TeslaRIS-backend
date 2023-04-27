package rs.teslaris.core.dto.person;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
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

    @NotBlank(message = "You have to provide a contact phone number.")
    private String phoneNumber;
}
