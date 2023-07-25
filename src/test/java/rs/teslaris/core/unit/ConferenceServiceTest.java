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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.service.impl.document.ConferenceServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceInUseException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class ConferenceServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private ConferenceServiceImpl conferenceService;


    @Test
    public void shouldReturnConferenceWhenItExists() {
        // given
        var expected = new Conference();
        when(conferenceRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = conferenceService.findConferenceById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenConferenceDoesNotExist() {
        // given
        when(conferenceRepository.findById(1)).thenReturn(Optional.empty());

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

        when(conferenceRepository.findAll(pageable)).thenReturn(
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
        conference1.setContributions(new HashSet<>());
        conference1.setFee("fee");
        conference1.setNumber("number");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference1));

        // when
        var actual = conferenceService.readConference(1);

        // then
        assertEquals("fee", actual.getFee());
        assertEquals("number", actual.getNumber());
    }

    @Test
    public void shouldThrowExceptionWhenConferenceDoesNotExist() {
        // given
        when(conferenceRepository.findById(1)).thenReturn(Optional.empty());

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

        when(conferenceRepository.save(any())).thenReturn(new Conference());

        // when
        var savedConference = conferenceService.createConference(conferenceDTO);

        // then
        assertNotNull(savedConference);
        verify(conferenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateConferenceWhenProvidedWithValidData() {
        // given
        var conference1 = new Conference();
        conference1.setName(new HashSet<>());
        conference1.setNameAbbreviation(new HashSet<>());
        conference1.setState(new HashSet<>());
        conference1.setPlace(new HashSet<>());
        conference1.setContributions(new HashSet<>());

        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        conferenceDTO.setPlace(new ArrayList<>());
        conferenceDTO.setState(new ArrayList<>());
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference1));
        when(conferenceRepository.save(any())).thenReturn(new Conference());

        // when
        conferenceService.updateConference(conferenceDTO, 1);

        // then
        verify(conferenceRepository, times(1)).save(any());
    }

    @Test
    public void shouldNotDeleteConferenceIfInUsage() {
        // given
        var conferenceId = 1;
        var conferenceToDelete = new Conference();

        when(conferenceRepository.findById(conferenceId)).thenReturn(
            Optional.of(conferenceToDelete));
        when(eventRepository.hasProceedings(conferenceId)).thenReturn(true);

        // when
        assertThrows(ConferenceInUseException.class,
            () -> conferenceService.deleteConference(conferenceId));

        // then (JournalInUseException should be thrown)
    }
}
