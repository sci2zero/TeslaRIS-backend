package rs.teslaris.reporting.service.interfaces;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;

@Service
public interface OrganisationUnitVisualizationDataService {

    List<YearlyCounts> getPublicationCountsForOrganisationUnit(
        Integer organisationUnitId,
        Integer startYear,
        Integer endYear);

    List<MCategoryCounts> getOrganisationUnitPublicationsByMCategories(
        Integer organisationUnitId, Integer startYear, Integer endYear);

    List<CommissionYearlyCounts> getMCategoryCountsForOrganisationUnit(
        Integer organisationUnitId, Integer startYear, Integer endYear);

    List<StatisticsByCountry> getByCountryStatisticsForOrganisationUnit(
        Integer organisationUnitId, LocalDate from, LocalDate to);

    Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer organisationUnitId, LocalDate from,
                                                    LocalDate to);

    Map<Year, Long> getYearlyStatisticsCounts(Integer organisationUnitId, Integer startYear,
                                              Integer endYear);
}
