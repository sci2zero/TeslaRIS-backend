package rs.teslaris.thesislibrary.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DissertationInformation {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> dissertationTitle = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_unit_id")
    private OrganisationUnit organisationUnit;

    @Column(name = "mentor")
    private String mentor;

    @Column(name = "commission")
    private String commission;

    @Column(name = "grade")
    private Integer grade;

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
