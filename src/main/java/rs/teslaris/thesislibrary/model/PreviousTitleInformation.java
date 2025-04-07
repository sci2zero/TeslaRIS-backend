package rs.teslaris.thesislibrary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PreviousTitleInformation {

    @Column(name = "institution_name")
    private String institutionName;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "institution_place")
    private String institutionPlace;

    @Column(name = "school_year")
    private String schoolYear;
}
