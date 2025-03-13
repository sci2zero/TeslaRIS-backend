package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;

@Service
public interface ThesisService {

    ThesisResponseDTO readThesisById(Integer thesisId);

    Thesis createThesis(ThesisDTO thesisDTO, Boolean index);

    void editThesis(Integer thesisId, ThesisDTO thesisDTO);

    void deleteThesis(Integer thesisId);

    void reindexTheses();

    DocumentFileResponseDTO addThesisAttachment(Integer thesisId, DocumentFileDTO document,
                                                ThesisAttachmentType attachmentType);

    void deleteThesisAttachment(Integer thesisId, Integer documentFileId,
                                ThesisAttachmentType attachmentType);

    void putOnPublicReview(Integer thesisId);
}
