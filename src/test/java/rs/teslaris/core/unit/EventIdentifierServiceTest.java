package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.dto.identifier.EventIdentifierDTO;
import rs.teslaris.core.model.commontypes.AccessLevel;
import rs.teslaris.core.model.commontypes.ApplicableEntityType;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.identifier.EventIdentifier;
import rs.teslaris.core.model.identifier.Identifier;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.EventIdentifierRepository;
import rs.teslaris.core.service.impl.identifier.EventIdentifierServiceImpl;
import rs.teslaris.core.service.impl.identifier.cruddelegate.EventIdentifierJPAServiceImpl;
import rs.teslaris.core.service.interfaces.document.EventLookupService;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class EventIdentifierServiceTest {

    @Mock
    private EventIdentifierRepository eventIdentifierRepository;

    @Mock
    private EventIdentifierJPAServiceImpl eventIdentifierJPAService;

    @Mock
    private EventLookupService eventLookupService;

    @Mock
    private EntityIdentifierRepository entityIdentifierRepository;

    @Mock
    private IdentifierService identifierService;

    @InjectMocks
    private EventIdentifierServiceImpl eventIdentifierService;


    @ParameterizedTest
    @EnumSource(value = AccessLevel.class, names = {"OPEN", "CLOSED", "ADMIN_ONLY"})
    void shouldReadAllEventIdentifiersForEvent(AccessLevel accessLevel) {
        // Given
        var eventId = 1;

        var identifier = new Identifier();
        identifier.setAccessLevel(accessLevel);
        identifier.setApplicableTypes(new HashSet<>(List.of(ApplicableEntityType.EVENT)));

        var eventIdentifier1 = new EventIdentifier();
        eventIdentifier1.setIdentifier(identifier);

        var eventIdentifier2 = new EventIdentifier();
        eventIdentifier2.setIdentifier(identifier);

        when(eventIdentifierRepository.findIdentifiersForEventAndIdentifierAccessLevel(eventId,
            accessLevel)).thenReturn(List.of(eventIdentifier1, eventIdentifier2));

        // When
        var response = eventIdentifierService.getIdentifiersForEvent(eventId, accessLevel);

        // Then
        assertNotNull(response);
        assertEquals(2, response.size());
        verify(eventIdentifierRepository)
            .findIdentifiersForEventAndIdentifierAccessLevel(eventId, accessLevel);
    }

    @Test
    void shouldReturnEmptyListWhenNoIdentifiersExistForEvent() {
        // Given
        var eventId = 1;

        when(eventIdentifierRepository.findIdentifiersForEventAndIdentifierAccessLevel(eventId,
            AccessLevel.OPEN)).thenReturn(List.of());

        // When
        var response = eventIdentifierService.getIdentifiersForEvent(eventId, AccessLevel.OPEN);

        // Then
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    void shouldCreateEventIdentifier() {
        // Given
        var eventIdentifierDTO = new EventIdentifierDTO();
        eventIdentifierDTO.setEventId(1);
        eventIdentifierDTO.setIdentifierId(1);
        eventIdentifierDTO.setValue("10.1234/test");

        var newEventIdentifier = new EventIdentifier();
        newEventIdentifier.setIdentifier(new Identifier());

        when(eventLookupService.fastEventLookup(1)).thenReturn(new Conference());
        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(eventIdentifierJPAService.save(any(EventIdentifier.class)))
            .thenReturn(newEventIdentifier);

        // When
        var result = eventIdentifierService.createEventIdentifier(eventIdentifierDTO, 1);

        // Then
        assertNotNull(result);
        verify(eventLookupService).fastEventLookup(1);
        verify(eventIdentifierJPAService).save(any(EventIdentifier.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingEventIdentifierWithNonExistentEvent() {
        // Given
        var eventIdentifierDTO = new EventIdentifierDTO();
        eventIdentifierDTO.setEventId(99);
        eventIdentifierDTO.setIdentifierId(1);

        when(identifierService.findOne(1)).thenReturn(new Identifier());
        when(eventLookupService.fastEventLookup(99)).thenThrow(
            new NotFoundException("Event not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            eventIdentifierService.createEventIdentifier(eventIdentifierDTO, 1));

        verify(eventIdentifierJPAService, never()).save(any());
    }

    @Test
    void shouldUpdateEventIdentifier() {
        // Given
        var eventIdentifierId = 1;
        var eventIdentifierDTO = new EventIdentifierDTO();
        eventIdentifierDTO.setEventId(1);
        eventIdentifierDTO.setIdentifierId(1);
        eventIdentifierDTO.setValue("10.1234/updated");

        var existingEventIdentifier = new EventIdentifier();
        existingEventIdentifier.setIdentifier(new Identifier());

        when(eventIdentifierJPAService.findOne(eventIdentifierId))
            .thenReturn(existingEventIdentifier);
        when(eventLookupService.fastEventLookup(1)).thenReturn(new Conference());
        when(identifierService.findOne(1)).thenReturn(new Identifier());

        // When
        eventIdentifierService.updateEventIdentifier(eventIdentifierId, eventIdentifierDTO);

        // Then
        verify(eventIdentifierJPAService).findOne(eventIdentifierId);
        verify(eventLookupService).fastEventLookup(1);
        verify(eventIdentifierJPAService).save(existingEventIdentifier);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentEventIdentifier() {
        // Given
        var eventIdentifierId = 99;
        var eventIdentifierDTO = new EventIdentifierDTO();
        eventIdentifierDTO.setEventId(1);
        eventIdentifierDTO.setIdentifierId(1);

        when(eventIdentifierJPAService.findOne(eventIdentifierId))
            .thenThrow(new NotFoundException("Event identifier not found."));

        // When & Then
        assertThrows(NotFoundException.class, () ->
            eventIdentifierService.updateEventIdentifier(eventIdentifierId, eventIdentifierDTO));

        verify(eventIdentifierJPAService, never()).save(any());
    }
}
