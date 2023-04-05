package rs.teslaris.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {

    private String email;

    private String oldPassword;

    private String newPassword;

    private String firstname;

    private String lastName;

    private Boolean canTakeRole;

    private Integer preferredLanguageId;

    private Integer personId;

    private Integer organisationalUnitId;
}
