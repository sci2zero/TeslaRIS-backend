package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.model.document.PersonEventContribution;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.document.EventServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

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

    @InjectMocks
    private EventServiceImpl eventService;


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
    @EnumSource(EventType.class)
    public void shouldFindConferenceWhenSearchingWithSimpleQuery(EventType eventType) {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result = eventService.searchEvents(tokens, pageable, eventType);

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
}
