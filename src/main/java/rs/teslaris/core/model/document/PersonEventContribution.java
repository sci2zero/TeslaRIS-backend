package rs.teslaris.core.model.document;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.MultiLingualContent;

@Getter
@Setter
@Entity
@Table(name = "person_event_contributions")
@SQLRestriction("deleted=false")
public class PersonEventContribution extends PersonContribution {

    @Column(name = "contribution_type", nullable = false)
    private EventContributionType contributionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> displayEvent = new HashSet<>();

    @Column(name = "lecture_hours_per_week")
    private String lectureHoursPerWeek;

    @Column(name = "tutorial_hours_per_week")
    private String tutorialHoursPerWeek;

    @Column(name = "lab_hours_per_week")
    private String labHoursPerWeek;

    @Column(name = "other_contact_hours_per_week")
    private String otherContactHoursPerWeek;

    @Column(name = "number_of_reviews_or_assessment")
    private Integer numberOfReviewsOrAssessment;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> caseName = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> locationJurisdiction = new HashSet<>();
}
