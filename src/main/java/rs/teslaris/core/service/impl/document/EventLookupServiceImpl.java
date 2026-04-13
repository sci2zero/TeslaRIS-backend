package rs.teslaris.core.service.impl.document;

import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Event;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.CourseRepository;
import rs.teslaris.core.repository.document.ExhibitionRepository;
import rs.teslaris.core.repository.document.OtherEventRepository;
import rs.teslaris.core.service.interfaces.document.EventLookupService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
public class EventLookupServiceImpl implements EventLookupService {

    private final EventIndexRepository eventIndexRepository;

    private final ConferenceRepository conferenceRepository;

    private final ExhibitionRepository exhibitionRepository;

    private final CourseRepository courseRepository;

    private final OtherEventRepository otherEventRepository;


    @Override
    public Event fastEventLookup(Integer eventId) {
        var index = getEventIndex(eventId);

        var event = getEventBasedOnIndex(index);

        if (Objects.isNull(event)) {
            throw throwNotFoundException();
        }

        return event;
    }

    @Override
    public Event fastEventLookup(EventIndex index) {
        var event = getEventBasedOnIndex(index);

        if (Objects.isNull(event)) {
            throw throwNotFoundException();
        }

        return event;
    }

    @Override
    public EventIndex getEventIndex(Integer eventId) {
        return eventIndexRepository.findByDatabaseId(eventId)
            .orElseThrow(this::throwNotFoundException);
    }

    @Nullable
    private Event getEventBasedOnIndex(EventIndex index) {
        if (Objects.isNull(index.getEventType())) {
            return null;
        }

        if (index.getEventType().equals(EventType.CONFERENCE)) {
            return conferenceRepository.findById(index.getDatabaseId())
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getEventType().equals(EventType.EXHIBITION)) {
            return exhibitionRepository.findById(index.getDatabaseId())
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getEventType().equals(EventType.COURSE)) {
            return courseRepository.findById(index.getDatabaseId())
                .orElseThrow(this::throwNotFoundException);
        } else if (index.getEventType().equals(EventType.OTHER_EVENT)) {
            return otherEventRepository.findById(index.getDatabaseId())
                .orElseThrow(this::throwNotFoundException);
        }

        return null;
    }

    private NotFoundException throwNotFoundException() {
        return new NotFoundException("Event with given ID is not present in the database.");
    }
}
