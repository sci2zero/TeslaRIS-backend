package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.service.interfaces.document.JournalService;

/**
 * Fills the ISSNs of a journal stub that was created from a name only.
 */
@Component
@RequiredArgsConstructor
public class HydratorJournalIssnWriter {

    private final JournalService journalService;


    @Transactional
    public void fill(Integer journalId, List<String> issns) {
        var journal = journalService.findJournalById(journalId);

        journal.setPrintISSN(issns.getFirst());
        if (issns.size() > 1) {
            journal.setEISSN(issns.get(1));
        }

        journalService.save(journal);
        journalService.indexJournal(journal);
    }
}
