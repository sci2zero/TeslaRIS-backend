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
public class BaseChartDisplaySettingsDTO {

    private ChartDisplaySettings publicationCountTotal;

    private ChartDisplaySettings publicationCountByYear;

    private ChartDisplaySettings publicationTypeByYear;

    private ChartDisplaySettings publicationCategoryByYear;

    private ChartDisplaySettings publicationTypeRatio;

    private ChartDisplaySettings publicationCategoryRatio;

    private ChartDisplaySettings viewCountTotal;

    private ChartDisplaySettings viewCountByMonth;

    private ChartDisplaySettings viewCountByCountry;
}
