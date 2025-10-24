package rs.teslaris.reporting.dto.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.reporting.model.ChartDisplaySettings;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DigitalLibraryChartDisplaySettingsDTO {

    private ChartDisplaySettings thesisCountTotal;

    private ChartDisplaySettings thesisCountByYear;

    private ChartDisplaySettings thesisTypeByYear;

    private ChartDisplaySettings thesisTypeRatio;

    private ChartDisplaySettings thesisViewCountTotal;

    private ChartDisplaySettings thesisViewCountByMonth;

    private ChartDisplaySettings thesisViewCountByCountry;

    private ChartDisplaySettings thesisDownloadCountTotal;

    private ChartDisplaySettings thesisDownloadCountByMonth;

    private ChartDisplaySettings thesisDownloadCountByCountry;

    private ChartDisplaySettings viewCountThesisLeaderboard;

    private ChartDisplaySettings downloadCountThesisLeaderboard;
}
