package rs.teslaris.core.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.core.assessment.model.EventIndicator;
import rs.teslaris.core.assessment.repository.EventIndicatorRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;

@Component
public class EventIndicatorJPAServiceImpl extends JPAServiceImpl<EventIndicator> {

    private final EventIndicatorRepository eventIndicatorRepository;

    @Autowired
    public EventIndicatorJPAServiceImpl(EventIndicatorRepository eventIndicatorRepository) {
        this.eventIndicatorRepository = eventIndicatorRepository;
    }

    @Override
    protected JpaRepository<EventIndicator, Integer> getEntityRepository() {
        return eventIndicatorRepository;
    }
}
