package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.JournalPublication;

@Service
public interface JournalPublicationService {

    JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId);

    JournalPublication createJournalPublication(JournalPublicationDTO journalPublicationDTO);

    void editJournalPublication(Integer publicationId, JournalPublicationDTO publicationDTO);

    void deleteJournalPublication(Integer journalPublicationId);

    void indexJournalPublication(JournalPublication publication, DocumentPublicationIndex index);
}
