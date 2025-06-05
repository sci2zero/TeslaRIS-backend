package rs.teslaris.thesislibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "You have to provide dissertation title.")
    private String dissertationTitle;

    private Integer organisationUnitId; // Ignored by backend

    private List<MultilingualContentDTO> institutionName;

    private String institutionPlace;

    @NotBlank(message = "You have to provide dissertation mentor information.")
    private String mentor;

    @NotBlank(message = "You have to provide dissertation commission information.")
    private String commission;

    private String grade;

    @NotBlank(message = "You have to provide acquired title.")
    private String acquiredTitle;

    @NotNull(message = "You have to provide defence date.")
    private LocalDate defenceDate;

    private String diplomaNumber;

    private LocalDate diplomaIssueDate;

    private String diplomaSupplementsNumber;

    private LocalDate diplomaSupplementsIssueDate;
}

