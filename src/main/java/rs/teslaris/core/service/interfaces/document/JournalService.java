package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.dto.document.JournalResponseDTO;
import rs.teslaris.core.model.document.Journal;

@Service
public interface JournalService {

    Page<JournalResponseDTO> readAllJournals(Pageable pageable);

    Journal findJournalById(Integer journalId);

    Journal createJournal(JournalDTO journalDTO);

    void updateJournal(JournalDTO journalDTO, Integer journalId);

    void deleteJournal(Integer journalId);
}