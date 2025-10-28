package rs.teslaris.core.repository.document;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentFile;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Integer> {

    @Query("SELECT df FROM DocumentFile df " +
        "JOIN FETCH df.document " +
        "WHERE df.serverFilename = :serverFilename")
    Optional<DocumentFile> getReferenceByServerFilename(String serverFilename);


    @Query("SELECT d.id FROM Document d " +
        "JOIN d.fileItems df " +
        "WHERE df.serverFilename = :filename")
    Integer getDocumentIdByFilename(String filename);

    @Query("SELECT t.id FROM Thesis t " +
        "JOIN t.preliminaryFiles pf " +
        "WHERE pf.serverFilename = :filename")
    Integer getThesisIdByFilename(String filename);

    Optional<DocumentFile> findDocumentFileByLegacyFilename(String legacyFilename);

    @Query("SELECT df FROM DocumentFile df WHERE " +
        "df.resourceType = 0 OR " +
        "df.resourceType = 1")
    Page<DocumentFile> findAllIndexable(Pageable pageable);
}
