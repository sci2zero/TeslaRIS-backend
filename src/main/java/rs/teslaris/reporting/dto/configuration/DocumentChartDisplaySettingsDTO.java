package rs.teslaris.reporting.dto.configuration;

import rs.teslaris.reporting.model.ChartDisplaySettings;

public record DocumentChartDisplaySettingsDTO(
    ChartDisplaySettings viewCountTotal,
    ChartDisplaySettings viewCountByMonth,
    ChartDisplaySettings downloadCountTotal,
    ChartDisplaySettings downloadCountByMonth,
    ChartDisplaySettings viewCountByCountry,
    ChartDisplaySettings downloadCountByCountry
) {
}
