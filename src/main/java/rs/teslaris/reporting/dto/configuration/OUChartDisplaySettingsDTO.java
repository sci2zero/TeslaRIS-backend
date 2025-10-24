package rs.teslaris.reporting.dto.configuration;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.reporting.model.ChartDisplaySettings;

@Getter
@Setter
@NoArgsConstructor
public class OUChartDisplaySettingsDTO extends BaseChartDisplaySettingsDTO {

    private ChartDisplaySettings publicationCountPersonLeaderboard;

    private ChartDisplaySettings publicationCountSubUnitLeaderboard;

    private ChartDisplaySettings citationCountPersonLeaderboard;

    private ChartDisplaySettings citationCountSubUnitLeaderboard;

    private ChartDisplaySettings citationCountDocumentLeaderboard;

    private ChartDisplaySettings assessmentPointPersonLeaderboard;

    private ChartDisplaySettings assessmentPointSubUnitLeaderboard;

    private ChartDisplaySettings viewCountPersonLeaderboard;

    private ChartDisplaySettings viewCountDocumentLeaderboard;

    private ChartDisplaySettings downloadCountDocumentLeaderboard;


    public OUChartDisplaySettingsDTO(
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
        ChartDisplaySettings viewCountByCountry,
        ChartDisplaySettings publicationCountPersonLeaderboard,
        ChartDisplaySettings publicationCountSubUnitLeaderboard,
        ChartDisplaySettings citationCountPersonLeaderboard,
        ChartDisplaySettings citationCountSubUnitLeaderboard,
        ChartDisplaySettings assessmentPointCountPersonLeaderboard,
        ChartDisplaySettings assessmentPointCountSubUnitLeaderboard,
        ChartDisplaySettings viewCountPersonLeaderboard,
        ChartDisplaySettings viewCountDocumentLeaderboard,
        ChartDisplaySettings downloadCountDocumentLeaderboard,
        ChartDisplaySettings citationCountDocumentLeaderboard) {

        super(publicationCountTotal, publicationCountByYear, publicationTypeByYear,
            publicationCategoryByYear, publicationTypeRatio, publicationCategoryRatio,
            citationCountTotal, citationCountByYear, viewCountTotal, viewCountByMonth,
            viewCountByCountry);

        this.publicationCountPersonLeaderboard = publicationCountPersonLeaderboard;
        this.publicationCountSubUnitLeaderboard = publicationCountSubUnitLeaderboard;
        this.citationCountPersonLeaderboard = citationCountPersonLeaderboard;
        this.citationCountSubUnitLeaderboard = citationCountSubUnitLeaderboard;
        this.assessmentPointPersonLeaderboard = assessmentPointCountPersonLeaderboard;
        this.assessmentPointSubUnitLeaderboard = assessmentPointCountSubUnitLeaderboard;
        this.viewCountPersonLeaderboard = viewCountPersonLeaderboard;
        this.viewCountDocumentLeaderboard = viewCountDocumentLeaderboard;
        this.downloadCountDocumentLeaderboard = downloadCountDocumentLeaderboard;
        this.citationCountDocumentLeaderboard = citationCountDocumentLeaderboard;
    }

    public OUChartDisplaySettingsDTO(BaseChartDisplaySettingsDTO other) {
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
            other.getCitationCountTotal() != null ?
                new ChartDisplaySettings(other.getCitationCountTotal()) : null,
            other.getCitationCountByYear() != null ?
                new ChartDisplaySettings(other.getCitationCountByYear()) : null,
            other.getViewCountTotal() != null ?
                new ChartDisplaySettings(other.getViewCountTotal()) : null,
            other.getViewCountByMonth() != null ?
                new ChartDisplaySettings(other.getViewCountByMonth()) : null,
            other.getViewCountByCountry() != null ?
                new ChartDisplaySettings(other.getViewCountByCountry()) : null
        );
    }

    public OUChartDisplaySettingsDTO(OUChartDisplaySettingsDTO other) {
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
            other.getCitationCountTotal() != null ?
                new ChartDisplaySettings(other.getCitationCountTotal()) : null,
            other.getCitationCountByYear() != null ?
                new ChartDisplaySettings(other.getCitationCountByYear()) : null,
            other.getViewCountTotal() != null ?
                new ChartDisplaySettings(other.getViewCountTotal()) : null,
            other.getViewCountByMonth() != null ?
                new ChartDisplaySettings(other.getViewCountByMonth()) : null,
            other.getViewCountByCountry() != null ?
                new ChartDisplaySettings(other.getViewCountByCountry()) : null
        );

        this.publicationCountPersonLeaderboard = other.publicationCountPersonLeaderboard != null ?
            new ChartDisplaySettings(other.publicationCountPersonLeaderboard) : null;
        this.publicationCountSubUnitLeaderboard = other.publicationCountSubUnitLeaderboard != null ?
            new ChartDisplaySettings(other.publicationCountSubUnitLeaderboard) : null;
        this.citationCountPersonLeaderboard = other.citationCountPersonLeaderboard != null ?
            new ChartDisplaySettings(other.citationCountPersonLeaderboard) : null;
        this.citationCountSubUnitLeaderboard = other.citationCountSubUnitLeaderboard != null ?
            new ChartDisplaySettings(other.citationCountSubUnitLeaderboard) : null;
        this.assessmentPointPersonLeaderboard =
            other.assessmentPointPersonLeaderboard != null ?
                new ChartDisplaySettings(other.assessmentPointPersonLeaderboard) : null;
        this.assessmentPointSubUnitLeaderboard =
            other.assessmentPointSubUnitLeaderboard != null ?
                new ChartDisplaySettings(other.assessmentPointSubUnitLeaderboard) : null;
        this.viewCountPersonLeaderboard =
            other.viewCountPersonLeaderboard != null ?
                new ChartDisplaySettings(other.viewCountPersonLeaderboard) : null;
        this.viewCountDocumentLeaderboard =
            other.viewCountDocumentLeaderboard != null ?
                new ChartDisplaySettings(other.viewCountDocumentLeaderboard) : null;
        this.downloadCountDocumentLeaderboard =
            other.downloadCountDocumentLeaderboard != null ?
                new ChartDisplaySettings(other.downloadCountDocumentLeaderboard) : null;
        this.citationCountDocumentLeaderboard =
            other.citationCountDocumentLeaderboard != null ?
                new ChartDisplaySettings(other.citationCountDocumentLeaderboard) : null;
    }
}
