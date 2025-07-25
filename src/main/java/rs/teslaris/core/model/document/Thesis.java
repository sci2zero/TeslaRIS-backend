package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
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
    private Set<MultiLingualContent> alternateTitle = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> extendedAbstract = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> remark = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> externalOrganisationUnitName = new HashSet<>();

    @Column(name = "thesis_type", nullable = false)
    private ThesisType thesisType;

    @Column(name = "topic_acceptance_date")
    private LocalDate topicAcceptanceDate;

    @Column(name = "thesis_defence_date")
    private LocalDate thesisDefenceDate;

    @Embedded
    private ThesisPhysicalDescription physicalDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;

    @JoinColumn(name = "scientific_area")
    private String scientificArea;

    @JoinColumn(name = "scientific_sub_field")
    private String scientificSubArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "writing_language_id")
    private LanguageTag writingLanguage;

    @Column(name = "is_on_public_review")
    private Boolean isOnPublicReview = false;

    @Column(name = "is_on_public_review_pause")
    private Boolean isOnPublicReviewPause = false;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<LocalDate> publicReviewStartDates = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> preliminaryFiles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> preliminarySupplements = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    private Set<DocumentFile> commissionReports = new HashSet<>();

    @Column(name = "place_of_keeping")
    private String placeOfKeeping;

    @Column(name = "e_isbn")
    private String eISBN;

    @Column(name = "print_isbn")
    private String printISBN;

    @Column(name = "udc")
    private String udc;

    @Column(name = "type_of_title")
    private String typeOfTitle;

    @Column(name = "public_review_completed")
    private Boolean publicReviewCompleted = false;
}
