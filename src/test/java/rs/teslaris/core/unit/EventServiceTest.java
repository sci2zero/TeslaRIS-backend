package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.dto.document.EventsRelationDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.EventsRelation;
import rs.teslaris.core.model.document.EventsRelationType;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.EventsRelationRepository;
import rs.teslaris.core.service.impl.document.EventServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.SelfRelationException;

@SpringBootTest
public class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private SearchService<EventIndex> searchService;

    @Mock
    private EventsRelationRepository eventsRelationRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    static Stream<Arguments> shouldFindConferenceWhenSearchingWithSimpleQuery_arguments() {
        return Stream.of(
            Arguments.of(EventType.CONFERENCE, true),
            Arguments.of(EventType.CONFERENCE, false)
        );
    }

    @Test
    public void shouldReturnEventWhenItExists() {
        // given
        var expected = new Conference();
        when(eventRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = eventService.findEventById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenEventDoesNotExist() {
        // given
        when(eventRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> eventService.findEventById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldSetCommonEventFieldsWhenProvidedWithValidData() {
        // given
        var conference = new Conference();
        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        conferenceDTO.setPlace(new ArrayList<>());
        conferenceDTO.setState(new ArrayList<>());
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());

        // when
        eventService.setEventCommonFields(conference, conferenceDTO);

        // then
        verify(personContributionService, times(1)).setPersonEventContributionForEvent(conference,
            conferenceDTO);
    }

    @Test
    public void shouldClearCommonFields() {
        var dummyMC = new MultiLingualContent(new LanguageTag(), "Content", 1);

        var conference = new Conference();
        conference.getName().add(dummyMC);
        conference.getDescription().add(dummyMC);
        conference.getKeywords().add(dummyMC);
        conference.getNameAbbreviation().add(dummyMC);
        conference.getState().add(dummyMC);
        conference.getPlace().add(dummyMC);
        conference.getContributions().add(new PersonEventContribution());

        eventService.clearEventCommonFields(conference);

        assertEquals(conference.getName().size(), 0);
        assertEquals(conference.getNameAbbreviation().size(), 0);
        assertEquals(conference.getState().size(), 0);
        assertEquals(conference.getPlace().size(), 0);
        assertEquals(conference.getContributions().size(), 0);
    }

    @ParameterizedTest
    @CsvSource("true,false")
    public void shouldGetCommonUsageWhenEventExists(Boolean hasProceedings) {
        var eventId = 1;
        when(eventRepository.hasProceedings(eventId)).thenReturn(hasProceedings);

        var result = eventService.hasCommonUsage(eventId);

        assertEquals(hasProceedings, result);
    }

    @ParameterizedTest
    @MethodSource("shouldFindConferenceWhenSearchingWithSimpleQuery_arguments")
    public void shouldFindConferenceWhenSearchingWithSimpleQuery(EventType eventType,
                                                                 boolean returnOnlySerialEvents) {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result = eventService.searchEvents(tokens, pageable, eventType, returnOnlySerialEvents);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldFindConferenceWhenSearchingWithImportQuery() {
        // Given
        var names = Arrays.asList("name 1", "name 2");

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result = eventService.searchEventsImport(names, "dateFrom", "dateTo");

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    void shouldFindEventByOldId() {
        // Given
        var oldId = 123;
        var expected = new Conference();
        when(eventRepository.findEventByOldId(oldId)).thenReturn(Optional.of(expected));

        // When
        var actual = eventService.findEventByOldId(oldId);

        // Then
        assertEquals(expected, actual);
        verify(eventRepository, times(1)).findEventByOldId(oldId);
    }

    @Test
    void shouldReturnNullWhenOldIdDoesNotExist() {
        // Given
        var oldId = 123;
        when(eventRepository.findEventByOldId(oldId)).thenReturn(Optional.empty());

        // When
        var actual = eventService.findEventByOldId(oldId);

        // Then
        assertNull(actual);
        verify(eventRepository, times(1)).findEventByOldId(oldId);
    }

    @Test
    public void shouldAddEventsRelationSuccessfully() {
        // Given
        var sourceEvent = new Conference();
        sourceEvent.setId(1);
        var targetEvent = new Conference();
        targetEvent.setId(2);
        targetEvent.setSerialEvent(true);

        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(2);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.BELONGS_TO_SERIES);

        when(eventRepository.findById(1)).thenReturn(Optional.of(sourceEvent));
        when(eventRepository.findById(2)).thenReturn(Optional.of(targetEvent));

        // When
        eventService.addEventsRelation(eventsRelationDTO);

        // Then
        verify(eventsRelationRepository, times(1)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldThrowExceptionWhenTargetEventIsNotSerialInBelongsToRelation() {
        // Given
        var sourceEvent = new Conference();
        sourceEvent.setId(1);
        var targetEvent = new Conference();
        targetEvent.setId(2);
        targetEvent.setSerialEvent(false);

        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(2);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.BELONGS_TO_SERIES);

        when(eventRepository.findById(1)).thenReturn(Optional.of(sourceEvent));
        when(eventRepository.findById(2)).thenReturn(Optional.of(targetEvent));

        // When & Then
        var exception =
            assertThrows(ConferenceReferenceConstraintViolationException.class, () -> {
                eventService.addEventsRelation(eventsRelationDTO);
            });

        assertEquals("targetEventNotSerialError", exception.getMessage());
        verify(eventsRelationRepository, times(0)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldThrowExceptionWhenSourceEventIsSerial() {
        // Given
        var sourceEvent = new Conference();
        sourceEvent.setId(1);
        sourceEvent.setSerialEvent(true);

        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(2);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.BELONGS_TO_SERIES);

        when(eventRepository.findById(1)).thenReturn(Optional.of(sourceEvent));

        // When & Then
        var exception =
            assertThrows(ConferenceReferenceConstraintViolationException.class, () -> {
                eventService.addEventsRelation(eventsRelationDTO);
            });

        assertEquals("Source event cannot be serial.", exception.getMessage());
        verify(eventsRelationRepository, times(0)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldThrowExceptionWhenTargetEventIsSameAsSourceEvent() {
        // Given
        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(1);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.PART_OF);

        // When & Then
        var exception =
            assertThrows(SelfRelationException.class, () -> {
                eventService.addEventsRelation(eventsRelationDTO);
            });

        assertEquals("selfRelationEventError", exception.getMessage());
        verify(eventsRelationRepository, times(0)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldThrowExceptionWhenRelationExists() {
        // Given
        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(2);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.PART_OF);

        when(eventsRelationRepository.relationExists(1, 2)).thenReturn(true);

        // When & Then
        var exception =
            assertThrows(ConferenceReferenceConstraintViolationException.class, () -> {
                eventService.addEventsRelation(eventsRelationDTO);
            });

        assertEquals("relationAlreadyExistsError", exception.getMessage());
        verify(eventsRelationRepository, times(0)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldAddEventsRelationForDifferentRelationType() {
        // Given
        var sourceEvent = new Conference();
        sourceEvent.setId(1);
        var targetEvent = new Conference();
        targetEvent.setId(2);

        var eventsRelationDTO = new EventsRelationDTO();
        eventsRelationDTO.setSourceId(1);
        eventsRelationDTO.setTargetId(2);
        eventsRelationDTO.setEventsRelationType(EventsRelationType.COLLOCATED_WITH);

        when(eventRepository.findById(1)).thenReturn(Optional.of(sourceEvent));
        when(eventRepository.findById(2)).thenReturn(Optional.of(targetEvent));

        // When
        eventService.addEventsRelation(eventsRelationDTO);

        // Then
        verify(eventsRelationRepository, times(1)).save(any(EventsRelation.class));
    }

    @Test
    public void shouldDeleteEventRelationSuccessfully() {
        // Given
        var relationId = 1;
        var relation = new EventsRelation();
        relation.setId(relationId);

        when(eventsRelationRepository.findById(relationId)).thenReturn(Optional.of(relation));

        // When
        eventService.deleteEventRelation(relationId);

        // Then
        verify(eventsRelationRepository, times(1)).delete(relation);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenRelationDoesNotExist() {
        // Given
        var relationId = 1;

        when(eventsRelationRepository.findById(relationId)).thenReturn(Optional.empty());

        // When
        var exception = assertThrows(NotFoundException.class, () -> {
            eventService.deleteEventRelation(relationId);
        });

        // Then
        assertEquals("Relation does not exist.", exception.getMessage());
        verify(eventsRelationRepository, times(0)).delete(any());
    }

    @Test
    void shouldReadEventRelations() {
        // Given
        var oneTimeEvent = new Conference();
        oneTimeEvent.setId(2);
        oneTimeEvent.setSerialEvent(false);

        var eventId = oneTimeEvent.getId();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(oneTimeEvent));
        when(eventsRelationRepository.getRelationsForOneTimeEvent(eventId)).thenReturn(
            Stream.of(
                new EventsRelation(EventsRelationType.PART_OF, oneTimeEvent, new Conference()),
                new EventsRelation(EventsRelationType.COLLOCATED_WITH, oneTimeEvent,
                    new Conference())).collect(Collectors.toList())
        );

        // When
        var result = eventService.readEventRelations(eventId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventsRelationRepository, times(1)).getRelationsForOneTimeEvent(eventId);
    }

    @Test
    void shouldThrowExceptionWhenReadingEventRelationsForSerialEvent() {
        // Given
        var serialEvent = new Conference();
        serialEvent.setId(1);
        serialEvent.setSerialEvent(true);

        var eventId = serialEvent.getId();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(serialEvent));

        // When
        var exception = assertThrows(NotFoundException.class, () -> {
            eventService.readEventRelations(eventId);
        });

        // Then
        assertEquals("One time event with this ID does not exist.", exception.getMessage());
    }

    @Test
    void shouldReadSerialEventRelations() {
        // Given
        var serialEvent = new Conference();
        serialEvent.setId(1);
        serialEvent.setSerialEvent(true);

        var serialEventId = serialEvent.getId();
        when(eventRepository.findById(serialEventId)).thenReturn(Optional.of(serialEvent));
        when(eventsRelationRepository.getRelationsForSerialEvent(serialEventId)).thenReturn(
            Stream.of(
                new EventsRelation(EventsRelationType.BELONGS_TO_SERIES, new Conference(),
                    serialEvent),
                new EventsRelation(EventsRelationType.BELONGS_TO_SERIES, new Conference(),
                    serialEvent)).collect(Collectors.toList())
        );

        // When
        var result = eventService.readSerialEventRelations(serialEventId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventsRelationRepository, times(1)).getRelationsForSerialEvent(serialEventId);
    }

    @Test
    void shouldThrowExceptionWhenReadingSerialEventRelationsForOneTimeEvent() {
        // Given
        var oneTimeEvent = new Conference();
        oneTimeEvent.setId(2);
        oneTimeEvent.setSerialEvent(false);

        var eventId = oneTimeEvent.getId();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(oneTimeEvent));

        // When
        var exception = assertThrows(NotFoundException.class, () -> {
            eventService.readSerialEventRelations(eventId);
        });

        // Then
        assertEquals("Serial event with this ID does not exist.", exception.getMessage());
    }
}
