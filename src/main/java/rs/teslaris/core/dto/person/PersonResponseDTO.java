package rs.teslaris.core.dto.person;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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

    private String imageServerFilename;

    private Boolean showFullBirthdate = true;


    public PersonResponseDTO(PersonResponseDTO other) {
        this.id = other.id;
        this.personName =
            Objects.nonNull(other.personName) ? new PersonNameDTO(other.personName) : null;
        this.personOtherNames = Objects.nonNull(other.personOtherNames)
            ? other.personOtherNames.stream().map(PersonNameDTO::new).collect(Collectors.toList())
            : null;
        this.personalInfo =
            Objects.nonNull(other.personalInfo) ? new PersonalInfoDTO(other.personalInfo) : null;
        this.biography = Objects.nonNull(other.biography)
            ? other.biography.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.keyword = Objects.nonNull(other.keyword)
            ? other.keyword.stream().map(MultilingualContentDTO::new).collect(Collectors.toList())
            : null;
        this.approveStatus = other.approveStatus;
        this.employmentIds =
            Objects.nonNull(other.employmentIds) ? new ArrayList<>(other.employmentIds) : null;
        this.educationIds =
            Objects.nonNull(other.educationIds) ? new ArrayList<>(other.educationIds) : null;
        this.membershipIds =
            Objects.nonNull(other.membershipIds) ? new ArrayList<>(other.membershipIds) : null;
        this.expertisesOrSkills = Objects.nonNull(other.expertisesOrSkills)
            ? other.expertisesOrSkills.stream().map(ExpertiseOrSkillResponseDTO::new)
            .collect(Collectors.toList())
            : null;
        this.prizes = Objects.nonNull(other.prizes)
            ? other.prizes.stream().map(PrizeResponseDTO::new).collect(Collectors.toList())
            : null;
        this.imageServerFilename = other.imageServerFilename;
        this.showFullBirthdate = other.showFullBirthdate;
    }
}
