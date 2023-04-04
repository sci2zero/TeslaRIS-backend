package rs.teslaris.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationRequestDTO {

    private String email;

    private String password;

    private String firstname;

    private String lastName;

    private Integer preferredLanguageId;

    private Integer authorityId;

    private Integer personId;

    private Integer organisationalUnitId;
}
