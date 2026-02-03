package rs.teslaris.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.Exhibition;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.ExhibitionRepository;
import rs.teslaris.core.service.impl.document.EventLookupServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
class EventLookupServiceTest {

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @InjectMocks
    private EventLookupServiceImpl eventLookupService;


    @Test
    public void shouldReturnEventWhenFoundForAllEventTypes() {
        // Given
        var eventId = 1;
        var conferenceIndex = createIndex(EventType.CONFERENCE);
        var conferenceEvent = new Conference();
        var exhibitionIndex = createIndex(EventType.EXHIBITION);
        var exhibitionEvent = new Exhibition();

        when(eventIndexRepository.findByDatabaseId(eventId))
            .thenReturn(Optional.of(conferenceIndex))
            .thenReturn(Optional.of(exhibitionIndex));

        when(conferenceRepository.findById(eventId)).thenReturn(Optional.of(conferenceEvent));
        when(exhibitionRepository.findById(eventId)).thenReturn(Optional.of(exhibitionEvent));

        // When & Then for conference
        var conferenceResult = eventLookupService.fastEventLookup(eventId);
        assertThat(conferenceResult).isEqualTo(conferenceEvent);
        assertThat(conferenceResult).isInstanceOf(Conference.class);

        // When & Then for exhibition
        var exhibitionResult = eventLookupService.fastEventLookup(eventId);
        assertThat(exhibitionResult).isEqualTo(exhibitionEvent);
        assertThat(exhibitionResult).isInstanceOf(Exhibition.class);
    }

    @Test
    public void shouldReturnEventWhenFoundForAllEventTypesByIndex() {
        // Given
        var eventId = 1;
        var eventIndex = mock(EventIndex.class);
        when(eventIndex.getEventType())
            .thenReturn(EventType.CONFERENCE)
            .thenReturn(EventType.EXHIBITION);
        when(eventIndex.getDatabaseId()).thenReturn(eventId);

        var conferenceEvent = new Conference();
        var exhibitionEvent = new Exhibition();

        when(conferenceRepository.findById(eventId)).thenReturn(Optional.of(conferenceEvent));
        when(exhibitionRepository.findById(eventId)).thenReturn(Optional.of(exhibitionEvent));

        // When & Then for conference
        var conferenceResult = eventLookupService.fastEventLookup(eventIndex);
        assertThat(conferenceResult).isEqualTo(conferenceEvent);

        // When & Then for exhibition
        var exhibitionResult = eventLookupService.fastEventLookup(eventIndex);
        assertThat(exhibitionResult).isEqualTo(exhibitionEvent);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenEventNotFoundForAllEventTypes() {
        // Given
        var eventId = 1;
        var conferenceIndex = createIndex(EventType.CONFERENCE);
        var exhibitionIndex = createIndex(EventType.EXHIBITION);

        when(eventIndexRepository.findByDatabaseId(eventId))
            .thenReturn(Optional.of(conferenceIndex))
            .thenReturn(Optional.of(exhibitionIndex));

        when(conferenceRepository.findById(eventId)).thenReturn(Optional.empty());
        when(exhibitionRepository.findById(eventId)).thenReturn(Optional.empty());

        // When & Then for conference
        assertThatThrownBy(() -> eventLookupService.fastEventLookup(eventId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Event with given ID is not present in the database.");

        // When & Then for exhibition
        assertThatThrownBy(() -> eventLookupService.fastEventLookup(eventId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Event with given ID is not present in the database.");
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenIndexNotFound() {
        // Given
        var eventId = 1;
        when(eventIndexRepository.findByDatabaseId(eventId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventLookupService.fastEventLookup(eventId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Event with given ID is not present in the database.");
    }

    @Test
    public void shouldReturnEventIndexWhenFound() {
        // Given
        var eventId = 1;
        var eventIndex = createIndex(EventType.CONFERENCE);
        when(eventIndexRepository.findByDatabaseId(eventId))
            .thenReturn(Optional.of(eventIndex));

        // When
        var result = eventLookupService.getEventIndex(eventId);

        // Then
        assertThat(result).isEqualTo(eventIndex);
        assertThat(result.getEventType()).isEqualTo(EventType.CONFERENCE);
        assertThat(result.getDatabaseId()).isEqualTo(eventId);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenGettingEventIndexAndIndexNotFound() {
        // Given
        var eventId = 1;
        when(eventIndexRepository.findByDatabaseId(eventId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventLookupService.getEventIndex(eventId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Event with given ID is not present in the database.");
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenEventTypeNotSupportedWithEventId() {
        // Given
        var eventId = 1;
        var eventIndex = mock(EventIndex.class);
        when(eventIndex.getEventType()).thenReturn(null);
        when(eventIndex.getDatabaseId()).thenReturn(eventId);
        when(eventIndexRepository.findByDatabaseId(eventId)).thenReturn(Optional.of(eventIndex));

        // When & Then
        assertThatThrownBy(() -> eventLookupService.fastEventLookup(eventId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Event with given ID is not present in the database.");
    }

    private EventIndex createIndex(EventType eventType) {
        var index = new EventIndex();
        index.setEventType(eventType);
        index.setDatabaseId(1);
        return index;
    }
}
