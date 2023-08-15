package rs.teslaris.core.service.impl.document.adapters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class ConferenceJPAServiceImpl extends JPAServiceImpl<Conference> {

    private final ConferenceRepository conferenceRepository;

    @Autowired
    public ConferenceJPAServiceImpl(ConferenceRepository conferenceRepository) {
        this.conferenceRepository = conferenceRepository;
    }

    @Override
    protected JpaRepository<Conference, Integer> getEntityRepository() {
        return conferenceRepository;
    }
}
