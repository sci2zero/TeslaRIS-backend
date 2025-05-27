package rs.teslaris.core.dto.user;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.user.UserNotificationPeriod;

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

    private String preferredUILanguage;

    private String preferredReferenceCataloguingLanguage;

    private Integer organisationUnitId;

    private Integer commissionId;

    private Integer personId;

    private List<MultilingualContentDTO> organisationUnitName;

    private UserNotificationPeriod notificationPeriod;
}
