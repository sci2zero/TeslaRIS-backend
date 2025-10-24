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
import rs.teslaris.reporting.service.interfaces.visualizations.PersonVisualizationDataService;

@RestController
@RequestMapping("/api/visualization-data/person")
@RequiredArgsConstructor
public class PersonVisualizationDataController {

    private final PersonVisualizationDataService personChartService;


    @GetMapping("/publication-count/{personId}")
    public List<YearlyCounts> getPublicationCountsForPerson(
        @PathVariable Integer personId,
        @RequestParam(required = false) Integer from,
        @RequestParam(required = false) Integer to) {
        return personChartService.getPublicationCountsForPerson(personId, from, to);
    }

    @GetMapping("/m-category/{personId}")
    public List<MCategoryCounts> getMCategoriesForPerson(
        @PathVariable Integer personId, @RequestParam Integer from, @RequestParam Integer to) {
        return personChartService.getPersonPublicationsByMCategories(personId, from, to);
    }

    @GetMapping("/m-category-count/{personId}")
    public List<CommissionYearlyCounts> getMCategoryCountsForPerson(
        @PathVariable Integer personId, @RequestParam Integer from, @RequestParam Integer to) {
        return personChartService.getMCategoryCountsForPerson(personId, from, to);
    }

    @GetMapping("/statistics/{personId}")
    public List<StatisticsByCountry> getViewsByCountryForPerson(
        @PathVariable Integer personId, @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate) {
        return personChartService.getByCountryStatisticsForPerson(personId, startDate, endDate);
    }

    @GetMapping("/monthly-statistics/{personId}")
    public Map<YearMonth, Long> getMonthlyViewsForPerson(@PathVariable Integer personId,
                                                         @RequestParam LocalDate startDate,
                                                         @RequestParam LocalDate endDate) {
        return personChartService.getMonthlyStatisticsCounts(personId, startDate, endDate);
    }

    @GetMapping("/yearly-citations/{personId}")
    public Map<Year, Long> getYearlyCitationsForPerson(@PathVariable Integer personId,
                                                       @RequestParam Integer from,
                                                       @RequestParam Integer to) {
        return personChartService.getYearlyCitationCounts(personId, from, to);
    }
}
