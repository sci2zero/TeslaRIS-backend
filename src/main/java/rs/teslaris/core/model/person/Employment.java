package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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
