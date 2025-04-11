package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import java.util.List;
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

    private String dissertationTitle;

    private Integer organisationUnitId; // Ignored by backend

    private List<MultilingualContentDTO> institutionName;

    private String institutionPlace;

    private String mentor;

    private String commission;

    private String grade;

    private String acquiredTitle;

    private LocalDate defenceDate;

    private String diplomaNumber;

    private LocalDate diplomaIssueDate;

    private String diplomaSupplementsNumber;

    private LocalDate diplomaSupplementsIssueDate;
}

