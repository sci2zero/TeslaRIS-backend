package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalRepository journalRepository;


    @Override
    public Journal findJournalById(Integer journalId) {
        return journalRepository.findById(journalId)
            .orElseThrow(() -> new NotFoundException("Journal with given id does not exist."));
    }
}
