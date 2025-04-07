package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DissertationInformationDTO {

    private List<MultilingualContentDTO> dissertationTitle;

    private Integer organisationUnitId;

    private String mentor;

    private String commission;

    private Integer grade;

    private String acquiredTitle;

    private LocalDate defenceDate;

    private String diplomaNumber;

    private LocalDate diplomaIssueDate;

    private String diplomaSupplementsNumber;

    private LocalDate diplomaSupplementsIssueDate;
}

