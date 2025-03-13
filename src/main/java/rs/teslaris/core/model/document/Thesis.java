package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.commontypes.ResearchArea;
import rs.teslaris.core.model.institution.OrganisationUnit;

@Getter
@Setter
@Entity
@Table(name = "theses")
@SQLRestriction("deleted=false")
public non-sealed class Thesis extends Document implements PublisherPublishable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_unit_id")
    private OrganisationUnit organisationUnit;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> externalOrganisationUnitName = new HashSet<>();

    @Column(name = "thesis_type", nullable = false)
    private ThesisType thesisType;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_area_id")
    private ResearchArea researchArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "writing_language_id")
    private LanguageTag writingLanguage;

    @Column(name = "is_on_public_review")
    private Boolean isOnPublicReview = false;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<LocalDate> publicReviewStartDates = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> preliminaryFiles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> preliminarySupplements = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> commissionReports = new HashSet<>();
}
