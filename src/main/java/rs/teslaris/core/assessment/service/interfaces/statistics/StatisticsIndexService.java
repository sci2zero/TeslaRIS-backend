package rs.teslaris.core.assessment.service.interfaces.statistics;

import org.springframework.stereotype.Service;

@Service
public interface StatisticsIndexService {

    void savePersonView(Integer personId);

    void saveDocumentView(Integer documentId);

    void saveOrganisationUnitView(Integer organisationUnitId);

    void saveDocumentDownload(Integer documentId);
}
