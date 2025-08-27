package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;
import rs.teslaris.core.service.impl.document.PersonVisualizationDataServiceImpl;

public interface PersonVisualizationDataService {

    List<PersonVisualizationDataServiceImpl.YearlyCounts> getPublicationCountsForPerson(
        Integer personId,
        Integer startYear,
        Integer endYear);

    List<PersonVisualizationDataServiceImpl.StatisticsByCountry> getByCountryStatisticsForPerson(
        Integer personId, StatisticsType statisticsType);
}
