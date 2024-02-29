package rs.teslaris.core.dto.person;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponseDTO {

    Integer id;

    PersonNameDTO personName;

    List<PersonNameDTO> personOtherNames;

    PersonalInfoDTO personalInfo;

    List<MultilingualContentDTO> biography;

    List<MultilingualContentDTO> keyword;

    ApproveStatus approveStatus;

    List<Integer> employmentIds;

    List<Integer> educationIds;

    List<Integer> membershipIds;

    List<ExpertiseOrSkillResponseDTO> expertisesOrSkills;
}
