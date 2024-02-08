package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.JournalPublication;

@Service
public interface JournalPublicationService {

    JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId);

    List<DocumentPublicationIndex> findMyPublicationsInJournal(Integer journalId, Integer authorId);

    List<DocumentPublicationIndex> findPublicationsInJournal(Integer journalId);

    JournalPublication createJournalPublication(JournalPublicationDTO journalPublicationDTO);

    void editJournalPublication(Integer publicationId, JournalPublicationDTO publicationDTO);

    void deleteJournalPublication(Integer journalPublicationId);

    void reindexJournalPublications();
}
