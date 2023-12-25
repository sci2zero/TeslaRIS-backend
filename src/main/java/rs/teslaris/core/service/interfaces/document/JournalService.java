package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface JournalService extends JPAService<Journal> {

    Page<JournalResponseDTO> readAllJournals(Pageable pageable);

    Page<JournalIndex> searchJournals(List<String> tokens, Pageable pageable);

    JournalResponseDTO readJournal(Integer journalId);

    Journal findJournalById(Integer journalId);

    Journal createJournal(JournalDTO journalDTO);

    Journal createJournal(JournalBasicAdditionDTO journalDTO);

    void updateJournal(JournalDTO journalDTO, Integer journalId);

    void deleteJournal(Integer journalId);
}
