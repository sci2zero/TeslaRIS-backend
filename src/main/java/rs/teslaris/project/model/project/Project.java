package rs.teslaris.project.model.project;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.project.model.common.MonetaryAmount;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.funding.FundingApplication;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicUpdate
@Table(name = "projects")
@SQLRestriction("deleted=false")
public class Project extends BaseEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "internal_identifiers")
    private Set<String> internalIdentifiers = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "old_ids")
    private Set<String> oldIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "merged_ids")
    private Set<String> mergedIds = new HashSet<>();

    @Column(name = "doi")
    private String doi;

    @Column(name = "raid")
    private String raid;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> name = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> nameAbbreviation = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> keywords = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> uris = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ProjectsRelation> relatedProjects = new HashSet<>();

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @BatchSize(size = 50)
    private Set<OrganisationUnitProjectContribution> consortium = new HashSet<>();

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private Set<PersonProjectContribution> team = new HashSet<>();

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private Set<ProjectDocument> documents = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<ProjectEvent> events = new HashSet<>();

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    @Column(name = "collaboration_type", nullable = false)
    private ProjectCollaborationType collaborationType;

    @Column(name = "research_type", nullable = false)
    private ProjectResearchType researchType;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Funding> funding = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY)
    private Set<FundingApplication> fundingApplications;

    @Column(name = "not_funded")
    private Boolean notFunded;

    @Embedded
    private MonetaryAmount costs;
}
