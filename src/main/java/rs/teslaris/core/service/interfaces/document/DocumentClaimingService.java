package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Service
public interface DocumentClaimingService {

    Page<DocumentPublicationIndex> findPotentialClaimsForPerson(Integer userId,
                                                                Pageable pageable);

    void claimDocument(Integer userId, Integer documentId);
}
