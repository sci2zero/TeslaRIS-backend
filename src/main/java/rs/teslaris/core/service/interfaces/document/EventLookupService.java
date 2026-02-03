package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.model.document.Event;

@Service
public interface EventLookupService {

    Event fastEventLookup(Integer eventId);

    Event fastEventLookup(EventIndex index);

    EventIndex getEventIndex(Integer eventId);
}
