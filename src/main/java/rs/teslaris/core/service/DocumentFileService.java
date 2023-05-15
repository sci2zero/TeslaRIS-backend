package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.model.document.DocumentFile;

@Service
public interface DocumentFileService {

    DocumentFile findDocumentFileById(Integer id);

    DocumentFile saveNewDocument(DocumentFileDTO documentFile);

    void editDocumentFile(DocumentFileDTO documentFile);

    void deleteDocumentFile(String serverFilename);
}
