package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.JPASoftDeleteRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.JournalService;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl extends JPAServiceImpl<Journal> implements JournalService {

    private final JournalRepository journalRepository;

    @Override
    protected JPASoftDeleteRepository<Journal> getEntityRepository() {
        return journalRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Journal findJournalById(Integer journalId) {
        return journalRepository.findByIdAndDeletedIsFalse(journalId)
            .orElseThrow(() -> new NotFoundException("Journal with given id does not exist."));
    }
}
