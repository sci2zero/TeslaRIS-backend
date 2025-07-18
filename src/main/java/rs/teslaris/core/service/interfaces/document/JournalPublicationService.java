package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.JournalPublication;

@Service
public interface JournalPublicationService {

    JournalPublication findJournalPublicationById(Integer publicationId);

    JournalPublicationResponseDTO readJournalPublicationById(Integer publicationId);

    List<DocumentPublicationIndex> findMyPublicationsInJournal(Integer journalId, Integer authorId);

    Page<DocumentPublicationIndex> findPublicationsInJournal(Integer journalId, Pageable pageable);

    JournalPublication createJournalPublication(JournalPublicationDTO journalPublicationDTO,
                                                Boolean index);

    void editJournalPublication(Integer publicationId, JournalPublicationDTO publicationDTO);

    void deleteJournalPublication(Integer journalPublicationId);

    void reindexJournalPublications();

    void indexJournalPublication(JournalPublication publication,
                                 DocumentPublicationIndex index);

    void indexJournalPublication(JournalPublication publication);
}
