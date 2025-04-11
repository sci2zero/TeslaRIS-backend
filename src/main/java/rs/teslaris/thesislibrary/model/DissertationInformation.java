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
public class DissertationInformation {

    @Column(name = "dissertation_title")
    private String dissertationTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_unit_id")
    private OrganisationUnit organisationUnit;

    @Column(name = "dissertation_institution_place")
    private String institutionPlace;

    @Column(name = "mentor")
    private String mentor;

    @Column(name = "commission")
    private String commission;

    @Column(name = "grade")
    private String grade;

    @Column(name = "acquired_title")
    private String acquiredTitle;

    @Column(name = "defence_date")
    private LocalDate defenceDate;

    @Column(name = "diploma_number")
    private String diplomaNumber;

    @Column(name = "diploma_issue_date")
    private LocalDate diplomaIssueDate;

    @Column(name = "diploma_supplements_number")
    private String diplomaSupplementsNumber;

    @Column(name = "diploma_supplements_issue_date")
    private LocalDate diplomaSupplementsIssueDate;
}
