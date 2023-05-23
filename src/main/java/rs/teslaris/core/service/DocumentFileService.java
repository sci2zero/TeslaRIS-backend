package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.indexmodel.DocumentFileIndex;
import rs.teslaris.core.model.document.DocumentFile;

@Service
public interface DocumentFileService {

    DocumentFile findDocumentFileById(Integer id);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile);

    void editDocumentFile(DocumentFileDTO documentFile);

    void deleteDocumentFile(String serverFilename);

    void parseAndIndexPdfDocument(DocumentFile documentFile, MultipartFile multipartPdfFile,
                                  String serverFilename, DocumentFileIndex documentIndex);
}
