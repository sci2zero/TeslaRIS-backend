package rs.teslaris.reporting.service.interfaces;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;

public interface PersonVisualizationDataService {

    List<YearlyCounts> getPublicationCountsForPerson(
        Integer personId,
        Integer startYear,
        Integer endYear);

    List<MCategoryCounts> getPersonPublicationsByMCategories(
        Integer personId, Integer startYear, Integer endYear);

    List<CommissionYearlyCounts> getMCategoryCountsForPerson(
        Integer personId, Integer startYear, Integer endYear);

    List<StatisticsByCountry> getByCountryStatisticsForPerson(
        Integer personId, LocalDate from, LocalDate to);

    Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer personId, LocalDate from, LocalDate to);
}
