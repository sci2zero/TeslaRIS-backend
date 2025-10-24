package rs.teslaris.reporting.controller.visualizations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.visualizations.DigitalLibraryVisualizationDataService;

@RestController
@RequestMapping("/api/visualization-data/digital-library")
@RequiredArgsConstructor
public class DigitalLibraryVisualizationController {

    private final DigitalLibraryVisualizationDataService digitalLibraryVisualizationDataService;


    @GetMapping("/thesis-count/{organisationUnitId}")
    public List<YearlyCounts> getPublicationCountsForOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestParam(required = false) Integer from,
        @RequestParam(required = false) Integer to,
        @RequestParam(required = false) List<ThesisType> allowedThesisTypes) {
        return digitalLibraryVisualizationDataService.getThesisCountsForOrganisationUnit(
            organisationUnitId, from, to, allowedThesisTypes);
    }

    @GetMapping("/monthly-statistics/{organisationUnitId}")
    public Map<YearMonth, Long> getMonthlyViewsForThesesFromOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestParam LocalDate from,
        @RequestParam LocalDate to,
        @RequestParam StatisticsType statisticsType,
        @RequestParam(required = false) List<ThesisType> allowedThesisTypes) {
        return digitalLibraryVisualizationDataService.getMonthlyStatisticsCounts(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);
    }

    @GetMapping("/statistics/{organisationUnitId}")
    public List<StatisticsByCountry> getStatisticsByCountryForOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestParam LocalDate from,
        @RequestParam LocalDate to,
        @RequestParam StatisticsType statisticsType,
        @RequestParam(required = false) List<ThesisType> allowedThesisTypes) {
        return digitalLibraryVisualizationDataService.getByCountryStatisticsForDigitalLibrary(
            organisationUnitId, from, to, statisticsType, allowedThesisTypes);
    }
}
