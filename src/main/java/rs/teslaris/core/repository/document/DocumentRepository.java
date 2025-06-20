package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    Optional<Document> findDocumentByOldId(Integer oldId);

    @Query("SELECT d FROM Document d " +
        "JOIN FETCH d.contributors " +
        "LEFT JOIN FETCH d.fileItems " +
        "LEFT JOIN FETCH d.proofs " +
        "WHERE d.id in :ids")
    List<Document> findDocumentByIdIn(List<Integer> ids, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.doi = :doi AND d.id <> :id")
    boolean existsByDoi(String doi, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.scopusId = :scopusId AND d.id <> :id")
    boolean existsByScopusId(String scopusId, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.openAlexId = :openAlexId AND d.id <> :id")
    boolean existsByOpenAlexId(String openAlexId, Integer id);

    @Query("SELECT d FROM Document d " +
        "JOIN PersonDocumentContribution dc ON d.id = dc.document.id " +
        "LEFT JOIN FETCH d.contributors " +
        "WHERE dc.person.id = :authorId")
    List<Document> getDocumentsForAuthorId(Integer authorId);

    @Query("SELECT DISTINCT inst.id " +
        "FROM ProceedingsPublication pp " +
        "JOIN pp.proceedings p " +
        "JOIN pp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE p.id = :proceedingsId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByProceedingsIdAndAuthorContribution(Integer proceedingsId);

    @Query("SELECT DISTINCT inst.id " +
        "FROM MonographPublication mp " +
        "JOIN mp.monograph m " +
        "JOIN mp.contributors pc " +
        "JOIN pc.institutions inst " +
        "WHERE m.id = :monographId " +
        "AND pc.contributionType = 0")
    Set<Integer> findInstitutionIdsByMonographIdAndAuthorContribution(Integer monographId);

    @Query("SELECT COUNT(d) > 0 FROM Document d LEFT JOIN d.fileItems fi WHERE " +
        "d.id = :documentId AND fi.resourceType = 1 AND fi.accessRights = 2")
    boolean isDocumentPubliclyAvailable(Integer documentId);

    @Query("SELECT d FROM Document d WHERE " +
        "d.openAlexId = :openAlexId OR " +
        "d.doi = :doi OR " +
        "d.scopusId = :scopusId")
    Optional<Document> findByOpenAlexIdOrDoiOrScopusId(String openAlexId, String doi,
                                                       String scopusId);
}
