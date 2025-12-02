package rs.teslaris.core.service.interfaces.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentFileService extends JPAService<DocumentFile> {

    DocumentFile findDocumentFileById(Integer id);

    DocumentFile getDocumentByServerFilename(String serverFilename);

    Optional<DocumentFileIndex> findDocumentFileIndexByDatabaseId(Integer databaseId);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index);

    DocumentFile saveNewPublicationDocument(DocumentFileDTO documentFile, Boolean index,
                                            Document document, boolean trusted);

    DocumentFile saveNewPersonalDocument(DocumentFileDTO documentFile, Boolean index,
                                         Person person);

    DocumentFile saveNewPreliminaryDocument(DocumentFileDTO documentFile);

    DocumentFileResponseDTO editDocumentFile(DocumentFileDTO documentFile, Boolean index);

    DocumentFileResponseDTO editDocumentFile(DocumentFileDTO documentFile, Boolean index,
                                             Integer documentId);

    void deleteDocumentFile(String serverFilename);

    void changeApproveStatus(Integer documentFileId, Boolean approved) throws IOException;

    void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                  String serverFilename, DocumentFileIndex documentIndex);

    void parseAndIndexPdfDocument(DocumentFile documentFile, InputStream inputStream,
                                  String documentTitle, String serverFilename,
                                  DocumentFileIndex documentIndex);

    Page<DocumentFileIndex> searchDocumentFiles(List<String> tokens, Pageable pageable,
                                                SearchRequestType type);

    void deleteIndexes();

    CompletableFuture<Void> reindexDocumentFiles();

    Integer findDocumentIdForFilename(String filename);

    DocumentFileIndex reindexDocumentFile(DocumentFile documentFile);
}
