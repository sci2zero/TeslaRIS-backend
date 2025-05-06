package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentFile;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Integer> {
    DocumentFile getReferenceByServerFilename(String serverFilename);

    @Query("SELECT d.id FROM Document d " +
        "JOIN d.fileItems df " +
        "WHERE df.serverFilename = :filename")
    Integer getDocumentIdByFilename(String filename);
}
