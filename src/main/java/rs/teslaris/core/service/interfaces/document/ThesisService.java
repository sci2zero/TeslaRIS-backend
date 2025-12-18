package rs.teslaris.core.service.interfaces.document;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DocumentFileDTO;
import rs.teslaris.core.dto.document.DocumentFileResponseDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisLibraryFormatsResponseDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.commontypes.RecurrenceType;
import rs.teslaris.core.model.document.LibraryFormat;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.model.document.ThesisAttachmentType;
import rs.teslaris.core.model.document.ThesisType;

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

    void putOnPublicReview(Integer thesisId, Boolean continueLastReview, Boolean shortened);

    void removeFromPublicReview(Integer thesisId);

    void archiveThesis(Integer thesisId);

    void unarchiveThesis(Integer thesisId);

    ThesisLibraryFormatsResponseDTO getLibraryReferenceFormat(Integer thesisId);

    String getSingleLibraryReferenceFormat(Integer thesisId, LibraryFormat libraryFormat);

    void transferPreliminaryFileToOfficial(Integer thesisId, Integer documentFileId);

    void schedulePublicReviewEndCheck(LocalDateTime timestamp, List<ThesisType> types,
                                      Integer publicReviewLengthDays, Integer userId,
                                      RecurrenceType recurrence);
}
