package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.interfaces.document.EventService;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;


    @Override
    public Event findEventById(Integer eventId) {
        return eventRepository.findById(eventId)
            .orElseThrow(() -> new NotFoundException("Event with given ID does not exist."));
    }
}
