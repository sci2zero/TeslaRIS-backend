package rs.teslaris.revisioner.util.dataquality;

import jakarta.annotation.Nullable;
import java.util.Objects;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public enum TrendMetric {

    OVERALL_SCORE(null),
    FAIR_COMPLIANCE(null),
    PUBLICATION_CANDIDATE_RATE(null),
    COMPLETENESS(QualityDimension.COMPLETENESS),
    VALIDITY(QualityDimension.VALIDITY),
    UNIQUENESS(QualityDimension.UNIQUENESS),
    CONSISTENCY(QualityDimension.CONSISTENCY),
    TIMELINESS(QualityDimension.TIMELINESS),
    ACCURACY(QualityDimension.ACCURACY),
    CONFORMITY(QualityDimension.CONFORMITY),
    INTEGRITY(QualityDimension.INTEGRITY);

    @Nullable
    private final QualityDimension dimension;


    TrendMetric(@Nullable QualityDimension dimension) {
        this.dimension = dimension;
    }

    @Nullable
    public QualityDimension dimension() {
        return dimension;
    }

    public boolean isDimension() {
        return Objects.nonNull(dimension);
    }

    public boolean isRate() {
        return this == PUBLICATION_CANDIDATE_RATE;
    }
}
