package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.revisioner.util.dataquality.TrendGranularity;
import rs.teslaris.revisioner.util.dataquality.TrendMetric;

public record QualityTrendDTO(

    TrendMetric metric,

    TrendGranularity granularity,

    List<TrendPointDTO> series,

    TrendIndicatorsDTO indicators,

    List<EntityTypeTrendDTO> trendByEntityType
) {
}
