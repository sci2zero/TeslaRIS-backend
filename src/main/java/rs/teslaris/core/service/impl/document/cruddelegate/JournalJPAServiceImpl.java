package rs.teslaris.core.service.impl.document.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class JournalJPAServiceImpl extends JPAServiceImpl<Journal> {

    private final JournalRepository journalRepository;

    @Autowired
    public JournalJPAServiceImpl(JournalRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    @Override
    protected JpaRepository<Journal, Integer> getEntityRepository() {
        return journalRepository;
    }
}
