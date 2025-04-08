package rs.teslaris.thesislibrary.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.thesislibrary.model.AcademicTitle;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PreviousTitleInformationDTO {

    private String institutionName;

    private LocalDate graduationDate;

    private String institutionPlace;

    private String schoolYear;

    private AcademicTitle academicTitle;
}

