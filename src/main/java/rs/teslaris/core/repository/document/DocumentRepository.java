package rs.teslaris.core.repository.document;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    Optional<Document> findDocumentByOldId(Integer oldId);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.doi = :doi AND d.id <> :id")
    boolean existsByDoi(String doi, Integer id);

    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN TRUE ELSE FALSE END " +
        "FROM Document d WHERE d.scopusId = :scopusId AND d.id <> :id")
    boolean existsByScopusId(String scopusId, Integer id);

    @Query("select d from Document d " +
        "join PersonDocumentContribution dc on d.id = dc.document.id " +
        "left join fetch d.contributors " +
        "where dc.person.id = :authorId")
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
        "d.id = :documentId AND fi.resourceType = 1 AND fi.license = 3")
    boolean isDocumentPubliclyAvailable(Integer documentId);
}
