package rs.teslaris.reporting.controller.visualizations;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;
import rs.teslaris.reporting.service.interfaces.OrganisationUnitVisualizationDataService;

@RestController
@RequestMapping("/api/visualization-data/organisation-unit")
@RequiredArgsConstructor
public class OrganisationUnitVisualizationDataController {

    private final OrganisationUnitVisualizationDataService organisationUnitVisualizationDataService;

    @GetMapping("/publication-count/{organisationUnitId}")
    public List<YearlyCounts> getPublicationCountsForOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestParam(required = false) Integer from,
        @RequestParam(required = false) Integer to) {
        return organisationUnitVisualizationDataService.getPublicationCountsForOrganisationUnit(
            organisationUnitId, from, to);
    }

    @GetMapping("/m-category/{organisationUnitId}")
    public List<MCategoryCounts> getMCategoriesForOrganisationUnit(
        @PathVariable Integer organisationUnitId, @RequestParam Integer from,
        @RequestParam Integer to) {
        return organisationUnitVisualizationDataService.getOrganisationUnitPublicationsByMCategories(
            organisationUnitId, from, to);
    }

    @GetMapping("/m-category-count/{organisationUnitId}")
    public List<CommissionYearlyCounts> getMCategoryCountsForOrganisationUnit(
        @PathVariable Integer organisationUnitId, @RequestParam Integer from,
        @RequestParam Integer to) {
        return organisationUnitVisualizationDataService.getMCategoryCountsForOrganisationUnit(
            organisationUnitId, from, to);
    }

    @GetMapping("/statistics/{organisationUnitId}")
    public List<StatisticsByCountry> getViewsByCountryForOrganisationUnit(
        @PathVariable Integer organisationUnitId, @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate) {
        return organisationUnitVisualizationDataService.getByCountryStatisticsForOrganisationUnit(
            organisationUnitId, startDate, endDate);
    }

    @GetMapping("/monthly-statistics/{organisationUnitId}")
    public Map<YearMonth, Long> getMonthlyViewsForOrganisationUnit(
        @PathVariable Integer organisationUnitId,
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate) {
        return organisationUnitVisualizationDataService.getMonthlyStatisticsCounts(
            organisationUnitId, startDate, endDate);
    }

    @GetMapping("/yearly-citations/{organisationUnitId}")
    public Map<Year, Long> getYearlyCitationsForPerson(@PathVariable Integer organisationUnitId,
                                                       @RequestParam Integer from,
                                                       @RequestParam Integer to) {
        return organisationUnitVisualizationDataService.getCitationsByYearForInstitution(
            organisationUnitId, from, to);
    }
}
