package rs.teslaris.core.controller.event;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import rs.teslaris.core.annotation.Idempotent;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.service.interfaces.document.EventService;

@RestController
@RequestMapping("/api/events-relation")
@RequiredArgsConstructor
@Traceable
public class EventsRelationController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public List<EventsRelationDTO> readRelationsForOneTimeEvent(@PathVariable Integer eventId) {
        return eventService.readEventRelations(eventId);
    }

    @GetMapping("/serial-event/{serialEventId}")
    public List<EventsRelationDTO> readRelationsForSerialEvent(
        @PathVariable Integer serialEventId) {
        return eventService.readSerialEventRelations(serialEventId);
    }

    @PatchMapping
    @Idempotent
    @PreAuthorize("hasAuthority('EDIT_EVENT_RELATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addEventsRelation(@RequestBody EventsRelationDTO eventsRelationDTO) {
        eventService.addEventsRelation(eventsRelationDTO);
    }

    @DeleteMapping("/{relationId}")
    @PreAuthorize("hasAuthority('EDIT_EVENT_RELATIONS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEventsRelation(@PathVariable Integer relationId) {
        eventService.deleteEventsRelation(relationId);
    }
}
