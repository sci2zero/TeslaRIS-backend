package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.document.ConferenceServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class ConferenceServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private ConferenceJPAServiceImpl conferenceJPAService;

    @Mock
    private SearchService<EventIndex> searchService;

    @Mock
    private EmailUtil emailUtil;

    @InjectMocks
    private ConferenceServiceImpl conferenceService;


    @Test
    public void shouldReturnConferenceWhenItExists() {
        // given
        var expected = new Conference();
        when(conferenceJPAService.findOne(1)).thenReturn(expected);

        // when
        var result = conferenceService.findConferenceById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenConferenceDoesNotExist() {
        // given
        when(conferenceJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class, () -> conferenceService.findConferenceById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReadAllConferences() {
        // given
        var pageable = Pageable.ofSize(5);
        var conference1 = new Conference();
        var conference2 = new Conference();

        when(conferenceJPAService.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(conference1, conference2)));
    }

    @Test
    public void shouldReadConferenceWhenExists() {
        // given
        var conference1 = new Conference();
        conference1.setFee("fee");
        conference1.setNumber("number");

        when(conferenceJPAService.findOne(1)).thenReturn(conference1);

        // when
        var actual = conferenceService.readConference(1);

        // then
        assertEquals("fee", actual.getFee());
        assertEquals("number", actual.getNumber());
    }

    @Test
    public void shouldThrowExceptionWhenConferenceDoesNotExist() {
        // given
        when(conferenceJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class, () -> conferenceService.readConference(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateConferenceWhenProvidedWithValidData() {
        // given
        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        conferenceDTO.setPlace(new ArrayList<>());
        conferenceDTO.setState(new ArrayList<>());
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());

        var conference = new Conference();
        conference.setDateFrom(LocalDate.now());
        conference.setDateTo(LocalDate.now());

        when(conferenceJPAService.save(any())).thenReturn(conference);

        // when
        var savedConference = conferenceService.createConference(conferenceDTO, true);

        // then
        assertNotNull(savedConference);
        verify(conferenceJPAService, times(1)).save(any());
    }

    @Test
    public void shouldCreateConferenceBasicWhenProvidedWithValidData() {
        // given
        var conferenceDTO = new ConferenceBasicAdditionDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());

        var conference = new Conference();
        conference.setId(1);
        conference.setDateFrom(LocalDate.now());
        conference.setDateTo(LocalDate.now());

        when(conferenceJPAService.save(any())).thenReturn(conference);

        // when
        var savedConference = conferenceService.createConference(conferenceDTO);

        // then
        assertNotNull(savedConference);
        verify(conferenceJPAService, times(1)).save(any());
        verify(emailUtil, times(1)).notifyInstitutionalEditor(1, "event");
    }

    @Test
    public void shouldUpdateConferenceWhenProvidedWithValidData() {
        // given
        var conference1 = new Conference();

        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        conferenceDTO.setDescription(new ArrayList<>());
        conferenceDTO.setKeywords(new ArrayList<>());
        conferenceDTO.setPlace(new ArrayList<>());
        conferenceDTO.setState(new ArrayList<>());
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());

        when(conferenceJPAService.findOne(1)).thenReturn(conference1);
        when(conferenceJPAService.save(any())).thenReturn(new Conference());

        // when
        conferenceService.updateConference(conferenceDTO, 1);

        // then
        verify(conferenceJPAService, times(1)).save(any());
    }

    @Test
    public void shouldNotDeleteConferenceIfInUsage() {
        // Given
        var conferenceId = 1;
        var conferenceToDelete = new Conference();

        when(conferenceRepository.findById(conferenceId)).thenReturn(
            Optional.of(conferenceToDelete));
        when(eventRepository.hasProceedings(conferenceId)).thenReturn(true);

        // When
        assertThrows(ConferenceReferenceConstraintViolationException.class,
            () -> conferenceService.deleteConference(conferenceId));

        // Then (JournalInUseException should be thrown)
    }

    @Test
    public void shouldFindConferenceWhenSearchingWithSimpleQuery() {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result =
            conferenceService.searchConferences(tokens, pageable);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexConferences() {
        // Given
        var conference1 = new Conference();
        conference1.setDateFrom(LocalDate.now());
        conference1.setDateTo(LocalDate.now());
        var conference2 = new Conference();
        conference2.setDateFrom(LocalDate.now());
        conference2.setDateTo(LocalDate.now());
        var conference3 = new Conference();
        conference3.setDateFrom(LocalDate.now());
        conference3.setDateTo(LocalDate.now());
        var conferences = Arrays.asList(conference1, conference2, conference3);
        var page1 =
            new PageImpl<>(conferences.subList(0, 2), PageRequest.of(0, 10), conferences.size());
        var page2 =
            new PageImpl<>(conferences.subList(2, 3), PageRequest.of(1, 10), conferences.size());

        when(conferenceJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        conferenceService.reindexConferences();

        // Then
        verify(eventIndexRepository, times(1)).deleteAll();
        verify(conferenceJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(eventIndexRepository, atLeastOnce()).save(any(EventIndex.class));
    }
}
