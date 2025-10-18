package rs.teslaris.reporting.service.interfaces.visualizations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.model.document.ThesisType;
import rs.teslaris.reporting.dto.YearlyCounts;

@Service
public interface DigitalLibraryVisualizationDataService {

    List<YearlyCounts> getThesisCountsForOrganisationUnit(
        Integer organisationUnitId,
        Integer startYear,
        Integer endYear,
        List<ThesisType> allowedThesisTypes);


    Map<YearMonth, Long> getMonthlyStatisticsCounts(Integer organisationUnitId, LocalDate from,
                                                    LocalDate to, StatisticsType statisticsType,
                                                    List<ThesisType> allowedThesisTypes);
}
