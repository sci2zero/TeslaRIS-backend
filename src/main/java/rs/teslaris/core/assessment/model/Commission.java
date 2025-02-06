package rs.teslaris.core.assessment.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "commissions")
@SQLRestriction("deleted=false")
public class Commission extends BaseEntity {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "commission_sources", joinColumns = @JoinColumn(name = "commission_id"))
    @Column(name = "source", nullable = false)
    private Set<String> sources = new HashSet<>();

    @Column(name = "assessment_date_from")
    private LocalDate assessmentDateFrom;

    @Column(name = "assessment_date_to")
    private LocalDate assessmentDateTo;

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Document> documentsForAssessment = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<Person> personsForAssessment = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<OrganisationUnit> organisationUnitsForAssessment = new HashSet<>();

    @Column(name = "formal_description_of_rule")
    private String formalDescriptionOfRule;

    @OneToMany(mappedBy = "sourceCommission", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<CommissionRelation> relations = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "commission_recognised_research_areas", joinColumns = @JoinColumn(name = "commission_id"))
    @Column(name = "recognised_research_areas", nullable = false)
    private Set<String> recognisedResearchAreas = new HashSet<>();
}
