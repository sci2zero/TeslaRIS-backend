package rs.teslaris.core.service;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.model.document.Document;

@Service
public interface DocumentPublicationService {

    Document findDocumentById(Integer documentId);

    void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved);

    void addDocumentFile(Integer documentId, List<DocumentFileDTO> documentFiles, Boolean isProof);

    void deleteDocumentFile(Integer documentId, Integer documentFileId, Boolean isProof);

    List<Integer> getContributorIds(Integer publicationId);
}
