package rs.teslaris.core.repository.document;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface DocumentFileRepository extends JPASoftDeleteRepository<DocumentFile> {

    @Deprecated(forRemoval = true)
    DocumentFile getReferenceByServerFilename(String serverFilename);


    DocumentFile getReferenceByServerFilenameAndDeletedIsFalse(String serverFilename);
}
