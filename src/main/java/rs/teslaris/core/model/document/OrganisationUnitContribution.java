package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.model.person.PersonName;
import rs.teslaris.project.model.funding.FundingPart;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "organisation_unit_contributions")
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class OrganisationUnitContribution extends BaseEntity {

    @Column(name = "favorite")
    private Boolean favorite = false;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> contributionDescription = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayOrganisationUnit = new HashSet<>();

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "display_contact_person_name_id")
    private PersonName displayContactPersonName;

    @Column(name = "order_number")
    private Integer orderNumber;

    @Column(name = "approve_status")
    private ApproveStatus approveStatus;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @Column(name = "is_main_contributor")
    private boolean isMainContributor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_person_id")
    private Person contactPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_unit_id")
    private OrganisationUnit organisationUnit;

    @OneToMany(mappedBy = "organisationUnitContribution", cascade = CascadeType.ALL,
        orphanRemoval = true)
    private Set<FundingPart> fundingParts = new HashSet<>();

    public OrganisationUnitContribution(Set<MultiLingualContent> contributionDescription,
                                        Set<MultiLingualContent> displayOrganisationUnit,
                                        PersonName displayContactPersonName,
                                        Integer orderNumber,
                                        ApproveStatus approveStatus,
                                        LocalDate dateFrom,
                                        LocalDate dateTo,
                                        Set<String> uris,
                                        Boolean favorite,
                                        boolean isMainContributor,
                                        Person contactPerson,
                                        OrganisationUnit organisationUnit) {
        this.contributionDescription = contributionDescription;
        this.displayOrganisationUnit = displayOrganisationUnit;
        this.displayContactPersonName = displayContactPersonName;
        this.orderNumber = orderNumber;
        this.approveStatus = approveStatus;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.uris = uris;
        this.favorite = favorite;
        this.isMainContributor = isMainContributor;
        this.contactPerson = contactPerson;
        this.organisationUnit = organisationUnit;
    }

}
