package rs.teslaris.core.service.interfaces.document;

import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.License;
import rs.teslaris.core.model.document.ResourceType;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.core.util.search.SearchRequestType;

@Service
public interface DocumentFileService extends JPAService<DocumentFile> {

    DocumentFile findDocumentFileById(Integer id);

    License getDocumentAccessLevel(String serverFilename);

    ResourceType getDocumentResourceType(String serverFilename);

    DocumentFileIndex findDocumentFileIndexByDatabaseId(Integer databaseId);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile, Boolean index);

    DocumentFileResponseDTO editDocumentFile(DocumentFileDTO documentFile, Boolean index);

    void deleteDocumentFile(String serverFilename);

    void changeApproveStatus(Integer documentFileId, Boolean approved) throws IOException;

    void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                  String serverFilename, DocumentFileIndex documentIndex);

    Page<DocumentFileIndex> searchDocumentFiles(List<String> tokens, Pageable pageable,
                                                SearchRequestType type);

    void deleteIndexes();

    Integer findDocumentIdForFilename(String filename);
}
