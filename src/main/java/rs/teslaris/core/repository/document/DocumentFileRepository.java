package rs.teslaris.core.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentFile;

@Repository
public interface DocumentFileRepository extends JpaRepository<DocumentFile, Integer> {
    DocumentFile getReferenceByServerFilename(String serverFilename);

    @Query("select d.id from Document d join d.fileItems df where df.serverFilename = :filename")
    Integer getDocumentIdByFilename(String filename);
}
