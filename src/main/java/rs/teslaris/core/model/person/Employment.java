package rs.teslaris.core.model.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Where;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "employments")
@Where(clause = "deleted=false")
public class Employment extends Involvement {

    @Column(name = "employment_position")
    private EmploymentPosition employmentPosition;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> role = new HashSet<>();

    public Employment(LocalDate dateFrom,
                      LocalDate dateTo,
                      ApproveStatus approveStatus,
                      Set<DocumentFile> proofs,
                      InvolvementType involvementType,
                      Set<MultiLingualContent> affiliationStatement,
                      Person personInvolved,
                      OrganisationUnit organisationUnit,
                      EmploymentPosition employmentPosition,
                      Set<MultiLingualContent> role) {
        super(dateFrom, dateTo, approveStatus, proofs, involvementType, affiliationStatement,
            personInvolved, organisationUnit);
        this.employmentPosition = employmentPosition;
        this.role = role;
    }
}
