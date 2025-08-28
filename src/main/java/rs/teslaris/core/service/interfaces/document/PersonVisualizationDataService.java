package rs.teslaris.core.service.interfaces.document;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import rs.teslaris.core.service.impl.document.PersonVisualizationDataServiceImpl;

public interface PersonVisualizationDataService {

    List<PersonVisualizationDataServiceImpl.YearlyCounts> getPublicationCountsForPerson(
        Integer personId,
        Integer startYear,
        Integer endYear);

    List<PersonVisualizationDataServiceImpl.StatisticsByCountry> getByCountryStatisticsForPerson(
        Integer personId);

    Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer personId, LocalDate from, LocalDate to);

    List<PersonVisualizationDataServiceImpl.MCategoryCounts> getPersonPublicationsByMCategories(
        Integer personId, Integer startYear, Integer endYear);

    List<PersonVisualizationDataServiceImpl.CommissionYearlyCounts> getMCategoryCountsForPerson(
        Integer personId, Integer startYear, Integer endYear);
}
