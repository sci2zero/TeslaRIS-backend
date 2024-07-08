package rs.teslaris.core.repository.document;

import java.util.Optional;
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
}
