package rs.teslaris.core.service.impl.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.DocumentAnalyticsService;

@Service
public class DocumentAnalyticsServiceImpl implements DocumentAnalyticsService {


    @Override
    public Page<DocumentPublicationIndex> findPublicationsForTypeAndPeriod(
        DocumentPublicationType type, Integer yearFrom, Integer yearTo, Integer personId,
        Integer institutionId, Pageable pageable) {
        // Concrete implementation is in reporting plugin
        return Page.empty();
    }
}
