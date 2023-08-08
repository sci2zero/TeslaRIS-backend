package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface DocumentFileService extends JPAService<DocumentFile> {

    DocumentFile findDocumentFileById(Integer id);

    DocumentFileIndex findDocumentFileIndexByDatabaseId(Integer databaseId);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index);

    void editDocumentFile(DocumentFileDTO documentFile);

    void deleteDocumentFile(String serverFilename);

    void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                  String serverFilename, DocumentFileIndex documentIndex);
}
