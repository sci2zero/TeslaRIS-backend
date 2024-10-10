package rs.teslaris.core.assessment.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "entity_assessment_classifications")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "entity_type", discriminatorType = DiscriminatorType.STRING)
public class EntityAssessmentClassification extends BaseEntity {

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "manual")
    private Boolean manual = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    private Commission commission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_classification_id")
    private AssessmentClassification assessmentClassification;
}