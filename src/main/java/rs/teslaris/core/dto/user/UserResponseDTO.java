package rs.teslaris.core.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private Integer id;

    private String email;

    private String firstname;

    private String lastName;

    private Boolean locked;

    private Boolean canTakeRole;

    private String preferredLanguage;
}
