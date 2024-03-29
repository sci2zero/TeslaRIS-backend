package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentPublicationService extends JPAService<Document> {

    Document findDocumentById(Integer documentId);

    Long getPublicationCount();

    void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved);

    void addDocumentFile(Integer documentId, List<DocumentFileDTO> documentFiles, Boolean isProof);

    void deleteDocumentFile(Integer documentId, Integer documentFileId, Boolean isProof);

    void deleteDocumentPublication(Integer documentId);

    List<Integer> getContributorIds(Integer publicationId);

    void indexCommonFields(Document document, DocumentPublicationIndex index);

    DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId);

    Page<DocumentPublicationIndex> searchDocumentPublications(List<String> tokens,
                                                              Pageable pageable,
                                                              SearchRequestType type);
}
