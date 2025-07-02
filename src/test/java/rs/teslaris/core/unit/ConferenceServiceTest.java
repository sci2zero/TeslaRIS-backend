package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.ConferenceBasicAdditionDTO;
import rs.teslaris.core.dto.document.ConferenceDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.document.Conference;
import rs.teslaris.core.repository.document.ConferenceRepository;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.ConferenceServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ConferenceJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.ConferenceReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
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

    @Mock
    private CountryService countryService;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private CommissionRepository commissionRepository;

    @InjectMocks
    private ConferenceServiceImpl conferenceService;

    static Stream<Arguments> shouldFindConferenceWhenSearchingWithSimpleQuery() {
        return Stream.of(
            Arguments.of(true, true, null, null),
            Arguments.of(true, false, 1, null),
            Arguments.of(false, true, null, 1),
            Arguments.of(false, false, 1, 1)
        );
    }

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
        conferenceDTO.setCountryId(1);
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());

        var conference = new Conference();
        conference.setDateFrom(LocalDate.now());
        conference.setDateTo(LocalDate.now());

        when(conferenceJPAService.save(any())).thenReturn(conference);
        when(countryService.findOne(1)).thenReturn(new Country());

        // when
        var savedConference = conferenceService.createConference(conferenceDTO, true);

        // then
        assertNotNull(savedConference);
        verify(conferenceJPAService, times(1)).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenNonSerialNotProvidedWithDates() {
        // given
        var conferenceDTO = new ConferenceDTO();
        conferenceDTO.setName(new ArrayList<>());
        conferenceDTO.setNameAbbreviation(new ArrayList<>());
        conferenceDTO.setPlace(new ArrayList<>());
        conferenceDTO.setSerialEvent(false);
        conferenceDTO.setContributions(new ArrayList<>());
        conferenceDTO.setCountryId(1);

        when(countryService.findOne(1)).thenReturn(new Country());

        // when & then
        assertThrows(MissingDataException.class,
            () -> conferenceService.createConference(conferenceDTO, true));

        // MissingDataException should be thrown
    }

    @Test
    public void shouldCreateConferenceBasicWhenProvidedWithValidData() {
        // given
        var conferenceDTO = new ConferenceBasicAdditionDTO();
        conferenceDTO.setName(
            new ArrayList<>(List.of(new MultilingualContentDTO(1, "EN", "Content", 1))));
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
    }

    @Test
    public void shouldReindexConference() {
        // given
        var conference = new Conference();
        conference.setId(1);
        conference.setDateFrom(LocalDate.now());
        conference.setDateTo(LocalDate.now());

        when(conferenceJPAService.findOne(1)).thenReturn(conference);
        when(eventIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(new EventIndex()));

        // when
        conferenceService.reindexConference(1);

        // then
        verify(eventIndexRepository, times(2)).save(any());
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
        conferenceDTO.setDateFrom(LocalDate.now());
        conferenceDTO.setDateTo(LocalDate.now());
        conferenceDTO.setContributions(new ArrayList<>());
        conferenceDTO.setCountryId(1);

        when(countryService.findOne(1)).thenReturn(new Country());
        when(conferenceJPAService.findOne(1)).thenReturn(conference1);
        when(conferenceJPAService.save(any())).thenReturn(new Conference());

        // when
        conferenceService.updateConference(1, conferenceDTO);

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

    @ParameterizedTest
    @MethodSource("shouldFindConferenceWhenSearchingWithSimpleQuery")
    public void shouldFindConferenceWhenSearchingWithSimpleQuery(boolean returnOnlyNonSerialEvents,
                                                                 boolean returnOnlySerialEvents,
                                                                 Integer commissionInstitutionId,
                                                                 Integer commissionId) {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result =
            conferenceService.searchConferences(tokens, pageable, returnOnlyNonSerialEvents,
                returnOnlySerialEvents, commissionInstitutionId, commissionId);

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

    @Test
    void shouldForceDeleteConference() {
        // Given
        var conferenceId = 1;

        when(eventIndexRepository.findByDatabaseId(conferenceId)).thenReturn(Optional.empty());

        // When
        conferenceService.forceDeleteConference(conferenceId);

        // Then
        verify(eventRepository).deleteAllPublicationsInEvent(conferenceId);
        verify(eventRepository).deleteAllProceedingsInEvent(conferenceId);
        verify(conferenceJPAService).delete(conferenceId);
        verify(documentPublicationIndexRepository).deleteByEventIdAndType(conferenceId,
            DocumentPublicationType.PROCEEDINGS.name());
        verify(eventIndexRepository, never()).delete(any());
    }

    @Test
    void shouldReturnConferenceDTOWhenOldIdExists() {
        // Given
        var oldId = 100;
        var conference = new Conference();
        conference.setId(1);
        conference.setOldId(oldId);

        var expectedDTO = new ConferenceDTO();
        expectedDTO.setId(1);
        expectedDTO.setOldId(oldId);

        when(conferenceRepository.findConferenceByOldId(oldId)).thenReturn(Optional.of(conference));

        // When
        var response = conferenceService.readConferenceByOldId(oldId);

        // Then
        assertNotNull(response);
        assertEquals(expectedDTO.getId(), response.getId());
        assertEquals(expectedDTO.getOldId(), response.getOldId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenOldIdDoesNotExist() {
        // Given
        var oldId = 200;
        when(conferenceRepository.findConferenceByOldId(oldId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> conferenceService.readConferenceByOldId(oldId));

        assertEquals("Conference with old ID " + oldId + " does not exist.",
            exception.getMessage());
    }

    @Test
    void shouldReturnFalseWhenConfIdDoesNotExist() {
        // given
        var identifier = "123456";
        var organisationUnitId = 1;
        when(eventRepository.existsByConfId(identifier, organisationUnitId)).thenReturn(false);

        // when
        var result = conferenceService.isIdentifierInUse(identifier, organisationUnitId);

        // then
        assertFalse(result);
        verify(eventRepository).existsByConfId(identifier, organisationUnitId);
    }

    @Test
    void shouldReturnTrueWhenConfIdExists() {
        // given
        var identifier = "123456";
        var organisationUnitId = 1;
        when(eventRepository.existsByConfId(identifier, organisationUnitId)).thenReturn(true);

        // when
        var result = conferenceService.isIdentifierInUse(identifier, organisationUnitId);

        // then
        assertTrue(result);
        verify(eventRepository).existsByConfId(identifier, organisationUnitId);
    }

    @Test
    void shouldReindexVolatileConferenceInformationWhenConferenceExists() {
        // Given
        var conferenceId = 123;
        var eventIndex = new EventIndex();
        var institutionIds = Set.of(1, 2, 3);

        when(eventIndexRepository.findByDatabaseId(conferenceId))
            .thenReturn(Optional.of(eventIndex));
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(conferenceId))
            .thenReturn(institutionIds);

        // When
        conferenceService.reindexVolatileConferenceInformation(conferenceId);

        // Then
        assertEquals(institutionIds.stream().toList(), eventIndex.getRelatedInstitutionIds());
        verify(eventIndexRepository).save(eventIndex);
    }

    @Test
    void shouldNotReindexWhenConferenceDoesNotExist() {
        // Given
        var conferenceId = 456;
        when(eventIndexRepository.findByDatabaseId(conferenceId))
            .thenReturn(Optional.empty());

        // When
        conferenceService.reindexVolatileConferenceInformation(conferenceId);

        // Then
        verify(eventRepository, never()).findInstitutionIdsByEventIdAndAuthorContribution(any());
        verify(eventIndexRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyInstitutionList() {
        // Given
        var conferenceId = 789;
        var eventIndex = new EventIndex();
        when(eventIndexRepository.findByDatabaseId(conferenceId))
            .thenReturn(Optional.of(eventIndex));
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(conferenceId))
            .thenReturn(Collections.emptySet());

        // When
        conferenceService.reindexVolatileConferenceInformation(conferenceId);

        // Then
        assertTrue(eventIndex.getRelatedInstitutionIds().isEmpty());
        verify(eventIndexRepository).save(eventIndex);
    }

    @Test
    void shouldUpdateFieldsAndSave_whenReindexVolatileConferenceInformation() {
        // Given
        var conferenceId = 3;
        var eventIndex = mock(EventIndex.class);
        when(eventIndexRepository.findByDatabaseId(conferenceId))
            .thenReturn(Optional.of(eventIndex));
        var institutionIds = Set.of(100, 200, 300);
        var classifiedBy = List.of(1, 2);
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(
            conferenceId)).thenReturn(institutionIds);
        when(commissionRepository.findCommissionsThatClassifiedEvent(conferenceId)).thenReturn(
            classifiedBy);

        // When
        conferenceService.reindexVolatileConferenceInformation(conferenceId);

        // Then
        verify(eventIndex).setRelatedInstitutionIds(institutionIds.stream().toList());
        verify(eventIndex).setClassifiedBy(classifiedBy);
        verify(eventIndexRepository).save(eventIndex);
    }
}
