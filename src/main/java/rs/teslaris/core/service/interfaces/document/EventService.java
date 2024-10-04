package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface EventService extends JPAService<Event> {

    Event findEventByOldId(Integer eventId);

    void setEventCommonFields(Event event, EventDTO eventDTO);

    void clearEventCommonFields(Event event);

    Boolean hasCommonUsage(Integer eventId);

    Page<EventIndex> searchEvents(List<String> tokens, Pageable pageable,
                                  EventType eventType, Boolean returnOnlyNonSerialEvents);

    Page<EventIndex> searchEventsImport(List<String> names, String dateFrom, String dateTo);

    List<EventsRelationDTO> readEventRelations(Integer eventId);

    List<EventsRelationDTO> readSerialEventRelations(Integer serialEventId);

    void addEventsRelation(EventsRelationDTO eventsRelationDTO);

    void deleteEventsRelation(Integer relationId);
}
