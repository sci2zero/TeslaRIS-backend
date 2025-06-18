package rs.teslaris.assessment.service.impl.cruddelegate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import rs.teslaris.assessment.model.indicator.EventIndicator;
import rs.teslaris.assessment.repository.indicator.EventIndicatorRepository;
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
