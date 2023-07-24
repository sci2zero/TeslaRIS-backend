package rs.teslaris.core.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.exception.NotFoundException;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.JournalService;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl extends JPAServiceImpl<Journal> implements JournalService {

    private final JournalRepository journalRepository;

    @Override
    protected JpaRepository<Journal, Integer> getEntityRepository() {
        return journalRepository;
    }

    @Override
    @Deprecated(forRemoval = true)
    public Journal findJournalById(Integer journalId) {
        return journalRepository.findById(journalId)
            .orElseThrow(() -> new NotFoundException("Journal with given id does not exist."));
    }
}
