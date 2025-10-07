package rs.teslaris.reporting.dto.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.reporting.model.ChartDisplaySettings;

@Getter
@Setter
@NoArgsConstructor
public class PersonChartDisplaySettingsDTO extends BaseChartDisplaySettingsDTO {

    private ChartDisplaySettings citationCountTotal;

    private ChartDisplaySettings citationCountByYear;


    public PersonChartDisplaySettingsDTO(ChartDisplaySettings publicationCountTotal,
                                         ChartDisplaySettings publicationCountByYear,
                                         ChartDisplaySettings publicationTypeByYear,
                                         ChartDisplaySettings publicationCategoryByYear,
                                         ChartDisplaySettings publicationTypeRatio,
                                         ChartDisplaySettings publicationCategoryRatio,
                                         ChartDisplaySettings citationCountTotal,
                                         ChartDisplaySettings citationCountByYear,
                                         ChartDisplaySettings viewCountTotal,
                                         ChartDisplaySettings viewCountByMonth,
                                         ChartDisplaySettings viewCountByCountry) {
        super(publicationCountTotal, publicationCountByYear, publicationTypeByYear,
            publicationCategoryByYear, publicationTypeRatio, publicationCategoryRatio,
            viewCountTotal, viewCountByMonth, viewCountByCountry);
        this.citationCountTotal = citationCountTotal;
        this.citationCountByYear = citationCountByYear;
    }

    public PersonChartDisplaySettingsDTO(BaseChartDisplaySettingsDTO other) {
        super(
            other.getPublicationCountTotal() != null ?
                new ChartDisplaySettings(other.getPublicationCountTotal()) : null,
            other.getPublicationCountByYear() != null ?
                new ChartDisplaySettings(other.getPublicationCountByYear()) : null,
            other.getPublicationTypeByYear() != null ?
                new ChartDisplaySettings(other.getPublicationTypeByYear()) : null,
            other.getPublicationCategoryByYear() != null ?
                new ChartDisplaySettings(other.getPublicationCategoryByYear()) : null,
            other.getPublicationTypeRatio() != null ?
                new ChartDisplaySettings(other.getPublicationTypeRatio()) : null,
            other.getPublicationCategoryRatio() != null ?
                new ChartDisplaySettings(other.getPublicationCategoryRatio()) : null,
            other.getViewCountTotal() != null ?
                new ChartDisplaySettings(other.getViewCountTotal()) : null,
            other.getViewCountByMonth() != null ?
                new ChartDisplaySettings(other.getViewCountByMonth()) : null,
            other.getViewCountByCountry() != null ?
                new ChartDisplaySettings(other.getViewCountByCountry()) : null
        );
    }
}
