package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentPublicationService extends JPAService<Document> {

    Document findDocumentById(Integer documentId);

    Document findDocumentByOldId(Integer documentId);

    Page<DocumentPublicationIndex> findResearcherPublications(Integer authorId, Pageable pageable);

    Page<DocumentPublicationIndex> findPublicationsForPublisher(Integer publisherId,
                                                                Pageable pageable);

    Page<DocumentPublicationIndex> findPublicationsForOrganisationUnit(Integer organisationUnitId,
                                                                       Pageable pageable);

    Long getPublicationCount();

    void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved);

    DocumentFileResponseDTO addDocumentFile(Integer documentId, DocumentFileDTO documentFile,
                                            Boolean isProof);

    void deleteDocumentFile(Integer documentId, Integer documentFileId);

    void deleteDocumentPublication(Integer documentId);

    List<Integer> getContributorIds(Integer publicationId);

    void indexCommonFields(Document document, DocumentPublicationIndex index);

    DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId);

    Page<DocumentPublicationIndex> searchDocumentPublications(List<String> tokens,
                                                              Pageable pageable,
                                                              SearchRequestType type);

    Page<DocumentPublicationIndex> findDocumentDuplicates(List<String> titles, String doi,
                                                          String scopusId);

    void deleteIndexes();

    void reorderDocumentContributions(Integer documentId, Integer contributionId,
                                      Integer oldContributionOrderNumber,
                                      Integer newContributionOrderNumber);
}
