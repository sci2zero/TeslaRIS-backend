package rs.teslaris.assessment.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.service.impl.document.PersonVisualizationDataServiceImpl;
import rs.teslaris.core.service.interfaces.document.PersonVisualizationDataService;

@RestController
@RequestMapping("/api/visualization-data")
@RequiredArgsConstructor
public class ChartDataController {

    private final PersonVisualizationDataService personChartService;


    @GetMapping("/person/publication-count/{personId}")
    public List<PersonVisualizationDataServiceImpl.YearlyCounts> getPublicationCountsForPerson(
        @PathVariable Integer personId) {
        return personChartService.getPublicationCountsForPerson(personId, null, null);
    }

    @GetMapping("/person/statistics/{personId}")
    public List<PersonVisualizationDataServiceImpl.StatisticsByCountry> getViewsByCountryForPerson(
        @PathVariable Integer personId) {
        return personChartService.getByCountryStatisticsForPerson(personId);
    }

    @GetMapping("/person/monthly-statistics/{personId}")
    public Map<YearMonth, Long> getMonthlyViewsForPerson(@PathVariable Integer personId) {
        return personChartService.getMonthlyStatisticsCounts(personId,
            LocalDate.now().minusMonths(12), LocalDate.now());
    }

    @GetMapping("/person/m-category/{personId}")
    public List<PersonVisualizationDataServiceImpl.MCategoryCounts> getMCategoriesForPerson(
        @PathVariable Integer personId) {
        return personChartService.getPersonPublicationsByMCategories(personId, 2010, 2025);
    }

    @GetMapping("/person/m-category-count/{personId}")
    public List<PersonVisualizationDataServiceImpl.CommissionYearlyCounts> getMCategoryCountsForPerson(
        @PathVariable Integer personId) {
        return personChartService.getMCategoryCountsForPerson(personId, null, null);
    }
}
