package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
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
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "memberships")
@SQLRestriction("deleted=false")
public class Membership extends Involvement {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> contributionDescription = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> role = new HashSet<>();

    public Membership(LocalDate dateFrom,
                      LocalDate dateTo,
                      ApproveStatus approveStatus,
                      Set<DocumentFile> proofs,
                      InvolvementType involvementType,
                      Set<MultiLingualContent> affiliationStatement,
                      Person personInvolved,
                      OrganisationUnit organisationUnit,
                      Set<MultiLingualContent> contributionDescription,
                      Set<MultiLingualContent> role) {
        super(dateFrom, dateTo, approveStatus, proofs, involvementType, affiliationStatement,
            personInvolved, organisationUnit);
        this.contributionDescription = contributionDescription;
        this.role = role;
    }
}
