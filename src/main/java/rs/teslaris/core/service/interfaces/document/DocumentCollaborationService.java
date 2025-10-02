package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Service
public interface DocumentCollaborationService {

    Page<DocumentPublicationIndex> findPublicationsForCollaboration(Integer sourcePersonId,
                                                                    Integer targetPersonId,
                                                                    String collaborationType,
                                                                    Pageable pageable);
}
