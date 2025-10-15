package rs.teslaris.reporting.service.interfaces;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.service.interfaces.document.DocumentAnalyticsService;
import rs.teslaris.reporting.dto.StatisticsByCountry;

@Service
public interface DocumentVisualizationDataService extends DocumentAnalyticsService {

    List<StatisticsByCountry> getByCountryStatisticsForDocument(Integer documentId, LocalDate from,
                                                                LocalDate to,
                                                                StatisticsType statisticsType);

    Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer documentId, LocalDate from,
                                                    LocalDate to, StatisticsType statisticsType);

    Map<Year, Long> getYearlyStatisticsCounts(Integer documentId, Integer startYear,
                                              Integer endYear, StatisticsType statisticsType);
}
