package rs.teslaris.thesislibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PreviousTitleInformation {

    @Column(name = "institution_name", length = 1024)
    private String institutionName;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "institution_place")
    private String institutionPlace;

    @Column(name = "school_year")
    private String schoolYear;

    @Column(name = "academic_title")
    private AcademicTitle academicTitle;
}
