package rs.teslaris.revisioner.util.dataquality;

import jakarta.annotation.Nullable;
import java.util.Objects;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

public enum TrendMetric {

    OVERALL_SCORE(null),
    FAIR_COMPLIANCE(null),
    PUBLICATION_CANDIDATE_RATE(null),
    ACCURACY(QualityDimension.ACCURACY),
    CONSISTENCY(QualityDimension.CONSISTENCY),
    LINEAGE(QualityDimension.LINEAGE),
    STRUCTURAL_CONSISTENCY(QualityDimension.STRUCTURAL_CONSISTENCY),
    QUALITATIVE(QualityDimension.QUALITATIVE),
    SEMANTIC(QualityDimension.SEMANTIC),
    CURRENCY(QualityDimension.CURRENCY);

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
