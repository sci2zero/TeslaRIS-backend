package rs.teslaris.core.model.document;

import jakarta.persistence.*;
import lombok.*;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
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

    @OneToMany(fetch = FetchType.LAZY)
    private Set<FundingPart> fundingParts = new HashSet<>();

}
