package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentFileService extends JPAService<DocumentFile> {

    DocumentFile findDocumentFileById(Integer id);

    DocumentFileIndex findDocumentFileIndexByDatabaseId(Integer databaseId);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index);

    void editDocumentFile(DocumentFileDTO documentFile);

    void deleteDocumentFile(String serverFilename);

    void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                  String serverFilename, DocumentFileIndex documentIndex);

    Page<DocumentFileIndex> searchDocumentFiles(List<String> tokens, Pageable pageable,
                                                SearchRequestType type);
}
