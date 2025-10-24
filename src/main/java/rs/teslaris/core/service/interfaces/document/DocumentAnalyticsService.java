package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.ThesisType;

public interface DocumentAnalyticsService {

    Page<DocumentPublicationIndex> findPublicationsForTypeAndPeriod(DocumentPublicationType type,
                                                                    ThesisType subtype,
                                                                    Integer yearFrom,
                                                                    Integer yearTo,
                                                                    Integer personId,
                                                                    Integer institutionId,
                                                                    Pageable pageable);
}
