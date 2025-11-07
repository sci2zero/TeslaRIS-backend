package rs.teslaris.core.service.impl.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.service.interfaces.document.DocumentCollaborationService;

@Service
public class DocumentCollaborationServiceImpl implements DocumentCollaborationService {

    @Override
    public Page<DocumentPublicationIndex> findPublicationsForCollaboration(Integer sourcePersonId,
                                                                           Integer targetPersonId,
                                                                           String collaborationType,
                                                                           Integer yearFrom,
                                                                           Integer yearTo,
                                                                           Pageable pageable) {
        // Concrete implementation is in reporting plugin
        return Page.empty();
    }
}
