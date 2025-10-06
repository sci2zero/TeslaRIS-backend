package rs.teslaris.reporting.dto;

import rs.teslaris.reporting.model.ChartDisplaySettings;

public record PersonChartDisplaySettingsDTO(
    ChartDisplaySettings publicationCountTotal,
    ChartDisplaySettings publicationCountByYear,
    ChartDisplaySettings publicationTypeByYear,
    ChartDisplaySettings publicationCategoryByYear,
    ChartDisplaySettings publicationTypeRatio,
    ChartDisplaySettings publicationCategoryRatio,
    ChartDisplaySettings citationCountTotal,
    ChartDisplaySettings citationCountByYear,
    ChartDisplaySettings viewCountTotal,
    ChartDisplaySettings viewCountByMonth,
    ChartDisplaySettings viewCountByCountry
) {
}
