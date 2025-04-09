package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DissertationInformationDTO {

    private String dissertationTitle;

    private Integer organisationUnitId;

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

