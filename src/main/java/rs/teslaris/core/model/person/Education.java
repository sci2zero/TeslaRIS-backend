package rs.teslaris.core.model.person;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
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
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "educations")
@SQLRestriction("deleted=false")
public class Education extends Involvement {

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> thesisTitle = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> title = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> abbreviationTitle = new HashSet<>();

    @Column(name = "degree_type")
    private DegreeType degreeType;

    @Column(name = "educational_status")
    private EducationStatus educationStatus;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> degreeCode = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> degreeClassification = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<ResearchArea> researchAreas = new HashSet<>();


    public Education(LocalDate dateFrom, LocalDate dateTo, ApproveStatus approveStatus,
                     Set<DocumentFile> proofs, InvolvementType involvementType,
                     Set<MultiLingualContent> affiliationStatement, Person personInvolved,
                     OrganisationUnit organisationUnit, Boolean favorite, Set<String> uris,
                     Set<MultiLingualContent> description, Set<MultiLingualContent> keywords,
                     Set<MultiLingualContent> thesisTitle, Set<MultiLingualContent> title,
                     Set<MultiLingualContent> abbreviationTitle, DegreeType degreeType,
                     EducationStatus educationStatus, Set<MultiLingualContent> degreeCode,
                     Set<MultiLingualContent> degreeClassification,
                     Set<ResearchArea> researchAreas) {
        super(dateFrom, dateTo, approveStatus, proofs, involvementType, affiliationStatement,
            personInvolved, organisationUnit, favorite, uris, description, keywords);
        this.thesisTitle = thesisTitle;
        this.title = title;
        this.abbreviationTitle = abbreviationTitle;
        this.degreeType = degreeType;
        this.educationStatus = educationStatus;
        this.degreeCode = degreeCode;
        this.degreeClassification = degreeClassification;
        this.researchAreas = researchAreas;
    }
}
