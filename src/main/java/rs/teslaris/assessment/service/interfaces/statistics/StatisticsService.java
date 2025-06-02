package rs.teslaris.assessment.service.interfaces.statistics;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.statistics.StatisticsType;

@Service
public interface StatisticsService {

    List<String> fetchStatisticsTypeIndicators(StatisticsType statisticsType);

    void savePublicationSeriesView(Integer publicationSeriesId);

    void saveEventView(Integer eventId);

    void savePersonView(Integer personId);

    void saveDocumentView(Integer documentId);

    void saveOrganisationUnitView(Integer organisationUnitId);

    void saveDocumentDownload(Integer documentId);
}
