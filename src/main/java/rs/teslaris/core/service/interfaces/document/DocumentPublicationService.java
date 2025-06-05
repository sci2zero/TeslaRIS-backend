package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.Pair;
import rs.teslaris.core.util.Triple;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentPublicationService extends JPAService<Document> {

    DocumentDTO readDocumentPublication(Integer documentId);

    Document findDocumentById(Integer documentId);

    Document findDocumentByOldId(Integer documentId);

    Page<DocumentPublicationIndex> findResearcherPublications(Integer authorId,
                                                              List<Integer> ignore,
                                                              Pageable pageable);

    List<Integer> getResearchOutputIdsForDocument(Integer documentId);

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

    void reindexDocumentVolatileInformation(Integer documentId);

    DocumentPublicationIndex findDocumentPublicationIndexByDatabaseId(Integer documentId);

    Page<DocumentPublicationIndex> searchDocumentPublications(List<String> tokens,
                                                              Pageable pageable,
                                                              SearchRequestType type,
                                                              Integer institutionId,
                                                              Integer commissionId,
                                                              List<DocumentPublicationType> allowedTypes);

    Page<DocumentPublicationIndex> findDocumentDuplicates(List<String> titles, String doi,
                                                          String scopusId);

    Page<DocumentPublicationIndex> findNonAffiliatedDocuments(Integer organisationUnitId,
                                                              Integer personId,
                                                              Pageable pageable);

    void massAssignContributionInstitution(Integer organisationUnitId, Integer personId,
                                           List<Integer> documentIds, Boolean deleteOthers);

    void deleteIndexes();

    void reorderDocumentContributions(Integer documentId, Integer contributionId,
                                      Integer oldContributionOrderNumber,
                                      Integer newContributionOrderNumber);

    void unbindResearcherFromContribution(Integer personId, Integer documentId);

    boolean isIdentifierInUse(String identifier, Integer documentPublicationId);

    Pair<Long, Long> getDocumentCountsBelongingToInstitution(Integer institutionId);

    Pair<Long, Long> getAssessedDocumentCountsForCommission(Integer institutionId,
                                                            Integer commissionId);

    List<Triple<String, List<MultilingualContentDTO>, String>> getSearchFields(
        Boolean onlyExportFields);

    List<Pair<String, Long>> getWordCloudForSingleDocument(Integer documentId,
                                                           boolean foreignLanguage);
}
