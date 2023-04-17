package rs.teslaris.core.model.person;

import java.time.LocalDate;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "involvements")
@Inheritance(strategy = InheritanceType.JOINED)
public class Involvement extends BaseEntity {

    @Column(name = "date_from")
    LocalDate dateFrom;

    @Column(name = "date_to")
    LocalDate dateTo;

    @Column(name = "approve_status", nullable = false)
    ApproveStatus approveStatus;

    @OneToMany(fetch = FetchType.LAZY)
    Set<DocumentFile> proofs;

    @Column(name = "involvement_type", nullable = false)
    InvolvementType involvementType;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    Set<MultiLingualContent> affiliationStatement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    Person personInvolved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_unit_id", nullable = true)
    OrganisationUnit organisationUnit;
}
