package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisLibraryFormatsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;

@Service
public interface ThesisService {

    Thesis getThesisById(Integer thesisId);

    ThesisResponseDTO readThesisById(Integer thesisId);

    ThesisResponseDTO readThesisByOldId(Integer oldId);

    Thesis createThesis(ThesisDTO thesisDTO, Boolean index);

    void editThesis(Integer thesisId, ThesisDTO thesisDTO);

    void deleteThesis(Integer thesisId);

    void reindexTheses();

    void indexThesis(Thesis thesis);

    DocumentFileResponseDTO addThesisAttachment(Integer thesisId, DocumentFileDTO document,
                                                ThesisAttachmentType attachmentType);

    void deleteThesisAttachment(Integer thesisId, Integer documentFileId,
                                ThesisAttachmentType attachmentType);

    void putOnPublicReview(Integer thesisId, Boolean continueLastReview);

    void removeFromPublicReview(Integer thesisId);

    void archiveThesis(Integer thesisId);

    void unarchiveThesis(Integer thesisId);

    ThesisLibraryFormatsResponseDTO getLibraryReferenceFormat(Integer thesisId);
}
