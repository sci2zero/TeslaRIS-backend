package rs.teslaris.assessment.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
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

    @GetMapping("/person/statistics/{statisticsType}/{personId}")
    public List<PersonVisualizationDataServiceImpl.StatisticsByCountry> getPublicationCountsForPerson(
        @PathVariable Integer personId, @PathVariable StatisticsType statisticsType) {
        return personChartService.getByCountryStatisticsForPerson(personId, statisticsType);
    }
}
