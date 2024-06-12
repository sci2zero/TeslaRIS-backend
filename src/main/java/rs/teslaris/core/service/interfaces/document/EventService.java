package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.document.Event;

@Service
public interface EventService {

    Event findEventById(Integer eventId);

    Event findEventByOldId(Integer eventId);

    void setEventCommonFields(Event event, EventDTO eventDTO);

    void clearEventCommonFields(Event event);

    Boolean hasCommonUsage(Integer eventId);

    Page<EventIndex> searchEvents(List<String> tokens, Pageable pageable,
                                  EventType eventType);
}
