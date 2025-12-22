package rs.teslaris.assessment.model.classification;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.institution.Commission;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "entity_assessment_classifications",
    indexes = {
        @Index(name = "idx_pub_series_comm_year",
            columnList = "publication_series_id, commission_id, classification_year"),
        @Index(name = "idx_pub_series_category_year_comm",
            columnList = "publication_series_id, category_identifier, classification_year, commission_id"),
        @Index(name = "idx_doc_comm", columnList = "document_id, commission_id"),
        @Index(name = "idx_doc_comm_manual", columnList = "document_id, commission_id, manual"),
        @Index(name = "idx_event_comm_year", columnList = "event_id, commission_id, classification_year"),
        @Index(
            name = "idx_psac_comm_type_series_year",
            columnList = "commission_id, entity_type, publication_series_id, classification_year")
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
public class EntityAssessmentClassification extends BaseEntity {

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "classification_year")
    private Integer classificationYear;

    @Column(name = "manual")
    private Boolean manual = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    private Commission commission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_classification_id")
    private AssessmentClassification assessmentClassification;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Set<MultiLingualContent> classificationReason = new HashSet<>();
}
