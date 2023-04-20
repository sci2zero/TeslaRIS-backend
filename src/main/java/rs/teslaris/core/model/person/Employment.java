package rs.teslaris.core.model.person;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employments")
public class Employment extends Involvement {

    @Column(name = "employment_position", nullable = false)
    private EmploymentPosition employmentPosition;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> role;

    public Employment(LocalDate dateFrom, LocalDate dateTo,
                      ApproveStatus approveStatus,
                      Set<DocumentFile> proofs,
                      InvolvementType involvementType,
                      Set<MultiLingualContent> affiliationStatement,
                      Person personInvolved,
                      OrganisationUnit organisationUnit,
                      EmploymentPosition employmentPosition, Set<MultiLingualContent> role) {
        super(dateFrom, dateTo, approveStatus, proofs, involvementType, affiliationStatement,
            personInvolved, organisationUnit);
        this.employmentPosition = employmentPosition;
        this.role = role;
    }
}
