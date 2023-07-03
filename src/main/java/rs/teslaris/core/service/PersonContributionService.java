package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.model.document.Document;

@Service
public interface PersonContributionService {

    void setPersonDocumentContributionsForDocument(Document document, DocumentDTO documentDTO);
}
