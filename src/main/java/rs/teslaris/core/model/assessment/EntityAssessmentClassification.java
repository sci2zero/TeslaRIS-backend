package rs.teslaris.core.model.assessment;

import jakarta.persistence.Column;
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
import org.hibernate.annotations.SQLRestriction;
import rs.teslaris.core.model.commontypes.BaseEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_assessment_classifications")
@SQLRestriction("deleted=false")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class EntityAssessmentClassification extends BaseEntity {

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "manual")
    private Boolean manual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id")
    private Commission commission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_classification_id")
    private AssessmentClassification assessmentClassification;
}
