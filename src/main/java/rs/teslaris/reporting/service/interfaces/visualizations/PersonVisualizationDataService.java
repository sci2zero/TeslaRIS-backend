package rs.teslaris.reporting.service.interfaces.visualizations;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.DocumentContributionType;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.reporting.dto.CommissionYearlyCounts;
import rs.teslaris.reporting.dto.MCategoryCounts;
import rs.teslaris.reporting.dto.PersonFeaturedInformationDTO;
import rs.teslaris.reporting.dto.StatisticsByCountry;
import rs.teslaris.reporting.dto.YearlyCounts;

@Service
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

    Map<Year, Long> getYearlyStatisticsCounts(Integer personId, Integer startYear, Integer endYear);

    Map<Year, Long> getYearlyCitationCounts(Integer personId, Integer startYear, Integer endYear);

    PersonFeaturedInformationDTO getPersonFeaturedInformation(Integer personId);

    Pair<Integer, Integer> getContributionYearRange(Integer personId,
                                                    Set<DocumentContributionType> contributionTypes);
}
