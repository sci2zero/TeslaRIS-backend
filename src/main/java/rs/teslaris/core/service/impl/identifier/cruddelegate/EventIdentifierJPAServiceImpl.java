package rs.teslaris.core.service.impl.identifier.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.model.identifier.EventIdentifier;
import rs.teslaris.core.repository.identifier.EventIdentifierRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class EventIdentifierJPAServiceImpl extends JPAServiceImpl<EventIdentifier> {

    private final EventIdentifierRepository eventIdentifierRepository;


    @Autowired
    public EventIdentifierJPAServiceImpl(EventIdentifierRepository eventIdentifierRepository) {
        this.eventIdentifierRepository = eventIdentifierRepository;
    }

    @Override
    protected JpaRepository<EventIdentifier, Integer> getEntityRepository() {
        return eventIdentifierRepository;
    }
}
