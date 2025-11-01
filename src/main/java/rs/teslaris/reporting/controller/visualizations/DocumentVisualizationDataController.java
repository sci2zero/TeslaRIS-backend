package rs.teslaris.reporting.controller.visualizations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.service.interfaces.visualizations.DocumentVisualizationDataService;

@RestController
@RequestMapping("/api/visualization-data/document")
@RequiredArgsConstructor
public class DocumentVisualizationDataController {

    private final DocumentVisualizationDataService documentVisualizationDataService;


    @GetMapping("/statistics/{documentId}/{statisticsType}")
    public List<StatisticsByCountry> getStatisticsByCountryForDocument(
        @PathVariable Integer documentId, @PathVariable StatisticsType statisticsType,
        @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return documentVisualizationDataService.getByCountryStatisticsForDocument(documentId,
            startDate, endDate, statisticsType);
    }

    @GetMapping("/monthly-statistics/{documentId}/{statisticsType}")
    public Map<YearMonth, Long> getMonthlyStatisticsForDocument(@PathVariable Integer documentId,
                                                                @PathVariable
                                                                StatisticsType statisticsType,
                                                                @RequestParam LocalDate startDate,
                                                                @RequestParam LocalDate endDate) {
        return documentVisualizationDataService.getMonthlyStatisticsCounts(documentId, startDate,
            endDate, statisticsType);
    }

    @GetMapping("/publications")
    public Page<DocumentPublicationIndex> getPublicationsForTypeAndPeriod(
        @RequestParam(required = false) DocumentPublicationType type,
        @RequestParam Integer yearFrom,
        @RequestParam Integer yearTo,
        @RequestParam(required = false) Integer personId,
        @RequestParam(required = false) Integer institutionId,
        @RequestParam(required = false) ThesisType subType,
        Pageable pageable) {
        return documentVisualizationDataService.findPublicationsForTypeAndPeriod(type, subType,
            yearFrom,
            yearTo, personId, institutionId, pageable);
    }
}
