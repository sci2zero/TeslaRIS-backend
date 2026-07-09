package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.project.model.funding.Funding;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "involvements",
    indexes = {
        @Index(name = "idx_involvement_person_id", columnList = "person_id"),
        @Index(name = "idx_involvement_org_unit_id", columnList = "organisation_unit_id"),
        @Index(name = "idx_involvement_type", columnList = "involvement_type"),
        @Index(
            name = "idx_involvement_type_orgunit",
            columnList = "involvement_type, organisation_unit_id"
        )
    }
)
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Involvement extends BaseEntity {

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "approve_status", nullable = false)
    private ApproveStatus approveStatus;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> proofs = new HashSet<>();

    @Column(name = "involvement_type", nullable = false)
    private InvolvementType involvementType;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> affiliationStatement = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", referencedColumnName = "id", nullable = false)
    private Person personInvolved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_unit_id")
    private OrganisationUnit organisationUnit;

    @Column(name = "favorite")
    private Boolean favorite = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @OneToMany(mappedBy = "involvement", fetch = FetchType.LAZY)
    private Set<Funding> fundings = new HashSet<>();

    public Involvement(LocalDate dateFrom, LocalDate dateTo, ApproveStatus approveStatus,
                       Set<DocumentFile> proofs, InvolvementType involvementType,
                       Set<MultiLingualContent> affiliationStatement, Person personInvolved,
                       OrganisationUnit organisationUnit, Boolean favorite, Set<String> uris,
                       Set<MultiLingualContent> description, Set<MultiLingualContent> keywords) {
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.approveStatus = approveStatus;
        this.proofs = proofs;
        this.involvementType = involvementType;
        this.affiliationStatement = affiliationStatement;
        this.personInvolved = personInvolved;
        this.organisationUnit = organisationUnit;
        this.favorite = favorite;
        this.uris = uris;
        this.description = description;
        this.keywords = keywords;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Involvement that = (Involvement) o;
        return Objects.equals(dateFrom, that.dateFrom) &&
            involvementType == that.involvementType &&
            Objects.equals(personInvolved, that.personInvolved) &&
            Objects.equals(organisationUnit, that.organisationUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dateFrom, involvementType, personInvolved,
            organisationUnit);
    }
}
