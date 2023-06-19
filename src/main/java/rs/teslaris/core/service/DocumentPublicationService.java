package rs.teslaris.core.service;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;

@Service
public interface DocumentPublicationService {

    Document findDocumentById(Integer documentId);

    JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId);

    JournalPublication createJournalPublication(JournalPublicationDTO journalPublicationDTO);

    void editJournalPublication(Integer publicationId, JournalPublicationDTO publicationDTO);

    void deleteJournalPublication(Integer journalPublicationId);

    void updateDocumentApprovalStatus(Integer documentId, Boolean isApproved);

    void addDocumentFile(Integer documentId, List<DocumentFileDTO> documentFiles, Boolean isProof);

    void deleteDocumentFile(Integer documentId, Integer documentFileId, Boolean isProof);
}
