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

    private Integer id;

    private PersonNameDTO personName;

    private List<PersonNameDTO> personOtherNames;

    private PersonalInfoDTO personalInfo;

    private List<MultilingualContentDTO> biography;

    private List<MultilingualContentDTO> keyword;

    private ApproveStatus approveStatus;

    private List<Integer> employmentIds;

    private List<Integer> educationIds;

    private List<Integer> membershipIds;

    private List<ExpertiseOrSkillResponseDTO> expertisesOrSkills;

    private List<PrizeResponseDTO> prizes;
}
