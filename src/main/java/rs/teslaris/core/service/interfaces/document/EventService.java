package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.Event;

@Service
public interface EventService {

    Event findEventById(Integer eventId);
}
