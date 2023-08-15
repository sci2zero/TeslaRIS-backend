package rs.teslaris.core.service.impl.document.adapters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class JournalPublicationJPAServiceImpl extends JPAServiceImpl<JournalPublication> {

    private final JournalPublicationRepository journalPublicationRepository;

    @Autowired
    public JournalPublicationJPAServiceImpl(JournalPublicationRepository conferenceRepository) {
        this.journalPublicationRepository = conferenceRepository;
    }

    @Override
    protected JpaRepository<JournalPublication, Integer> getEntityRepository() {
        return journalPublicationRepository;
    }
}
