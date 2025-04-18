package rs.teslaris.thesislibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "You have to provide previous title's institution.")
    private String institutionName;

    @NotNull(message = "you have to provide previous graduation date.")
    private LocalDate graduationDate;

    @NotBlank(message = "You have to provide previous title's institution place.")
    private String institutionPlace;

    @NotNull(message = "You have to provide previous title's school year.")
    private String schoolYear;

    @NotNull(message = "You have to provide previous academic title.")
    private AcademicTitle academicTitle;
}

