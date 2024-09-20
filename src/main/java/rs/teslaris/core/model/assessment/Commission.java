package rs.teslaris.core.model.assessment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
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
public class Commission extends OrganisationUnit {

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "commission_sources", joinColumns = @JoinColumn(name = "commission_id"))
    @Column(name = "source", nullable = false)
    private Set<String> sources;

    @Column(name = "assessment_date_from")
    private LocalDate assessmentDateFrom;

    @Column(name = "assessment_date_to")
    private LocalDate assessmentDateTo;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Document> documentsForAssessment;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Person> personsForAssessment;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<OrganisationUnit> organisationUnitsForAssessment;

    @Column(name = "formal_description_of_rule")
    private String formalDescriptionOfRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "super_commission")
    private Commission superComission;
}
