package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "person_contributions")
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class PersonContribution extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "person_id")
    private Person person;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> contributionDescription = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "affiliation_statement_id")
    private AffiliationStatement affiliationStatement;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "person_contribution_institutions",
        joinColumns = @JoinColumn(name = "person_contribution_id"),
        inverseJoinColumns = @JoinColumn(name = "institution_id"),
        indexes = {
            @Index(name = "idx_pci_pcid", columnList = "person_contribution_id"),
            @Index(name = "idx_pci_instid", columnList = "institution_id")
        }
    )
    @BatchSize(size = 50)
    private Set<OrganisationUnit> institutions = new HashSet<>();

    @Column(name = "order_number", nullable = false)
    private Integer orderNumber;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;
}
