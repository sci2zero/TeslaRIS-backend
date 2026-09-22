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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.project.model.funding.FundingPart;

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

    @OneToOne(
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
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

    @Column(name = "favorite")
    private Boolean favorite = false;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @Column(name = "main_contributor", nullable = false)
    private Boolean isMainContributor = false;

    @Column(name = "invited_contributor")
    private Boolean isInvitedContributor = false;

    @OneToMany(mappedBy = "personContribution", cascade = CascadeType.ALL,
        orphanRemoval = true)
    private Set<FundingPart> fundingParts = new HashSet<>();

    public PersonContribution(Person person,
                              Set<MultiLingualContent> contributionDescription,
                              AffiliationStatement affiliationStatement,
                              Set<OrganisationUnit> institutions,
                              Integer orderNumber,
                              ApproveStatus approveStatus, Boolean isMainContributor,
                              Boolean isInvitedContributor) {
        this.person = person;
        this.contributionDescription = contributionDescription;
        this.affiliationStatement = affiliationStatement;
        this.institutions = institutions;
        this.orderNumber = orderNumber;
        this.approveStatus = approveStatus;
        this.isMainContributor = isMainContributor;
        this.isInvitedContributor = isInvitedContributor;
    }

}
