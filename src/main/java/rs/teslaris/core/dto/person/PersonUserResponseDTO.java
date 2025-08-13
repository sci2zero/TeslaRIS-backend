package rs.teslaris.core.dto.person;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.user.UserResponseDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonUserResponseDTO {

    Integer personId;

    PersonNameDTO personName;

    List<PersonNameDTO> personOtherNames;

    PersonalInfoDTO personalInfo;

    List<MultilingualContentDTO> biography;

    List<MultilingualContentDTO> keyword;

    ApproveStatus approveStatus;

    UserResponseDTO user;
}
