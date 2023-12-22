package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
        conference1.setName(new HashSet<>());
        conference1.setNameAbbreviation(new HashSet<>());
        conference1.setState(new HashSet<>());
        conference1.setPlace(new HashSet<>());
        conference1.setContributions(new HashSet<>());
        var conference2 = new Conference();
        conference2.setName(new HashSet<>());
        conference2.setNameAbbreviation(new HashSet<>());
        conference2.setState(new HashSet<>());
        conference2.setPlace(new HashSet<>());
        conference2.setContributions(new HashSet<>());

        when(conferenceJPAService.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(conference1, conference2)));
    }

    @Test
    public void shouldReadConferenceWhenExists() {
        // given
        var conference1 = new Conference();
        conference1.setName(new HashSet<>());
        conference1.setNameAbbreviation(new HashSet<>());
        conference1.setState(new HashSet<>());
        conference1.setPlace(new HashSet<>());
        conference1.setDescription(new HashSet<>());
        conference1.setKeywords(new HashSet<>());
        conference1.setContributions(new HashSet<>());
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

        when(conferenceJPAService.save(any())).thenReturn(new Conference());

        // when
        var savedConference = conferenceService.createConference(conferenceDTO);

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
        conference1.setName(new HashSet<>());
        conference1.setNameAbbreviation(new HashSet<>());
        conference1.setDescription(new HashSet<>());
        conference1.setKeywords(new HashSet<>());
        conference1.setState(new HashSet<>());
        conference1.setPlace(new HashSet<>());
        conference1.setContributions(new HashSet<>());

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
}
