package rs.teslaris.assessment.model.classification;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.model.person.Prize;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DiscriminatorValue("PRIZE_ASSESSMENT_CLASS")
public class PrizeAssessmentClassification extends EntityAssessmentClassification {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id")
    private Prize prize;
}
