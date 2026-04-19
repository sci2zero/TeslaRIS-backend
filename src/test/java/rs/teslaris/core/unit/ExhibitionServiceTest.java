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
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.model.commontypes.Country;
import rs.teslaris.core.model.document.Exhibition;
import rs.teslaris.core.repository.document.EventRepository;
import rs.teslaris.core.repository.document.ExhibitionRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.ExhibitionServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.ExhibitionJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.CountryService;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.MissingDataException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class ExhibitionServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private ExhibitionRepository exhibitionRepository;

    @Mock
    private ExhibitionJPAServiceImpl exhibitionJPAService;

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
    private ExhibitionServiceImpl exhibitionService;


    static Stream<Arguments> shouldFindExhibitionWhenSearchingWithSimpleQuery() {
        return Stream.of(
            Arguments.of(true, true, null, null, null, null),
            Arguments.of(true, false, 1, null, null, null),
            Arguments.of(false, true, null, 1, true, false),
            Arguments.of(false, false, 1, 1, false, true)
        );
    }

    @Test
    public void shouldReturnExhibitionWhenItExists() {
        // given
        var expected = new Exhibition();
        when(exhibitionJPAService.findOne(1)).thenReturn(expected);

        // when
        var result = exhibitionService.findExhibitionById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenExhibitionDoesNotExist() {
        // given
        when(exhibitionJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class, () -> exhibitionService.findExhibitionById(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldReadAllExhibitions() {
        // given
        var pageable = Pageable.ofSize(5);
        var exhibition1 = new Exhibition();
        var exhibition2 = new Exhibition();

        when(exhibitionJPAService.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(exhibition1, exhibition2)));
    }

    @Test
    public void shouldReadExhibitionWhenExists() {
        // given
        var exhibition1 = new Exhibition();
        exhibition1.setFee("fee");
        exhibition1.setNumber("number");

        when(exhibitionJPAService.findOne(1)).thenReturn(exhibition1);

        // when
        var actual = exhibitionService.readExhibition(1);

        // then
        assertEquals("fee", actual.getFee());
        assertEquals("number", actual.getNumber());
    }

    @Test
    public void shouldThrowExceptionWhenExhibitionDoesNotExist() {
        // given
        when(exhibitionJPAService.findOne(1)).thenThrow(NotFoundException.class);

        // when
        assertThrows(NotFoundException.class, () -> exhibitionService.readExhibition(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateExhibitionWhenProvidedWithValidData() {
        // given
        var exhibitionDTO = new ExhibitionDTO();
        exhibitionDTO.setName(new ArrayList<>());
        exhibitionDTO.setNameAbbreviation(new ArrayList<>());
        exhibitionDTO.setPlace(new ArrayList<>());
        exhibitionDTO.setCountryId(1);
        exhibitionDTO.setDateFrom(LocalDate.now());
        exhibitionDTO.setDateTo(LocalDate.now());
        exhibitionDTO.setContributions(new ArrayList<>());

        var exhibition = new Exhibition();
        exhibition.setDateFrom(LocalDate.now());
        exhibition.setDateTo(LocalDate.now());

        when(exhibitionJPAService.save(any())).thenReturn(exhibition);
        when(countryService.findOne(1)).thenReturn(new Country());

        // when
        var savedExhibition = exhibitionService.createExhibition(exhibitionDTO, true);

        // then
        assertNotNull(savedExhibition);
        verify(exhibitionJPAService, times(1)).save(any());
    }

    @Test
    public void shouldThrowExceptionWhenNonSerialNotProvidedWithDates() {
        // given
        var exhibitionDTO = new ExhibitionDTO();
        exhibitionDTO.setName(new ArrayList<>());
        exhibitionDTO.setNameAbbreviation(new ArrayList<>());
        exhibitionDTO.setPlace(new ArrayList<>());
        exhibitionDTO.setSerialEvent(false);
        exhibitionDTO.setContributions(new ArrayList<>());
        exhibitionDTO.setCountryId(1);

        when(countryService.findOne(1)).thenReturn(new Country());

        // when & then
        assertThrows(MissingDataException.class,
            () -> exhibitionService.createExhibition(exhibitionDTO, true));

        // MissingDataException should be thrown
    }

    @Test
    public void shouldReindexExhibition() {
        // given
        var exhibition = new Exhibition();
        exhibition.setId(1);
        exhibition.setDateFrom(LocalDate.now());
        exhibition.setDateTo(LocalDate.now());

        when(exhibitionJPAService.findOne(1)).thenReturn(exhibition);
        when(eventIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(new EventIndex()));

        // when
        exhibitionService.reindexExhibition(1);

        // then
        verify(eventIndexRepository, times(2)).save(any());
    }

    @Test
    public void shouldUpdateExhibitionWhenProvidedWithValidData() {
        // given
        var exhibition1 = new Exhibition();

        var exhibitionDTO = new ExhibitionDTO();
        exhibitionDTO.setName(new ArrayList<>());
        exhibitionDTO.setNameAbbreviation(new ArrayList<>());
        exhibitionDTO.setDescription(new ArrayList<>());
        exhibitionDTO.setKeywords(new ArrayList<>());
        exhibitionDTO.setPlace(new ArrayList<>());
        exhibitionDTO.setDateFrom(LocalDate.now());
        exhibitionDTO.setDateTo(LocalDate.now());
        exhibitionDTO.setContributions(new ArrayList<>());
        exhibitionDTO.setCountryId(1);

        when(countryService.findOne(1)).thenReturn(new Country());
        when(exhibitionJPAService.findOne(1)).thenReturn(exhibition1);
        when(exhibitionJPAService.save(any())).thenReturn(new Exhibition());

        // when
        exhibitionService.updateExhibition(1, exhibitionDTO);

        // then
        verify(exhibitionJPAService, times(1)).save(any());
    }

    @ParameterizedTest
    @MethodSource("shouldFindExhibitionWhenSearchingWithSimpleQuery")
    public void shouldFindExhibitionWhenSearchingWithSimpleQuery(boolean returnOnlyNonSerialEvents,
                                                                 boolean returnOnlySerialEvents,
                                                                 Integer commissionInstitutionId,
                                                                 Integer commissionId,
                                                                 Boolean emptyEventsOnly,
                                                                 Boolean noContributionEventsOnly) {
        // Given
        var tokens = Arrays.asList("ključna", "ријеч", "keyword");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new EventIndex(), new EventIndex())));

        // When
        var result =
            exhibitionService.searchExhibitions(tokens, pageable, returnOnlyNonSerialEvents,
                returnOnlySerialEvents, commissionInstitutionId, commissionId,
                emptyEventsOnly, noContributionEventsOnly);

        // Then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexExhibitions() {
        // Given
        var exhibition1 = new Exhibition();
        exhibition1.setDateFrom(LocalDate.now());
        exhibition1.setDateTo(LocalDate.now());
        var exhibition2 = new Exhibition();
        exhibition2.setDateFrom(LocalDate.now());
        exhibition2.setDateTo(LocalDate.now());
        var exhibition3 = new Exhibition();
        exhibition3.setDateFrom(LocalDate.now());
        exhibition3.setDateTo(LocalDate.now());
        var exhibitions = Arrays.asList(exhibition1, exhibition2, exhibition3);
        var page1 =
            new PageImpl<>(exhibitions.subList(0, 2), PageRequest.of(0, 10), exhibitions.size());
        var page2 =
            new PageImpl<>(exhibitions.subList(2, 3), PageRequest.of(1, 10), exhibitions.size());

        when(exhibitionJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        exhibitionService.reindexExhibitions();

        // Then
        verify(eventIndexRepository, never()).deleteAll();
        verify(exhibitionJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(eventIndexRepository, atLeastOnce()).save(any(EventIndex.class));
    }

    @Test
    void shouldForceDeleteExhibition() {
        // Given
        var exhibitionId = 1;

        when(eventIndexRepository.findByDatabaseId(exhibitionId)).thenReturn(Optional.empty());

        // When
        exhibitionService.forceDeleteExhibition(exhibitionId);

        // Then
        verify(eventRepository, never()).deleteAllPublicationsInEvent(exhibitionId);
        verify(eventRepository, never()).deleteAllProceedingsInEvent(exhibitionId);
        verify(exhibitionJPAService).delete(exhibitionId);
        verify(documentPublicationIndexRepository).deleteByEventIdAndType(exhibitionId,
            DocumentPublicationType.PROCEEDINGS.name());
        verify(eventIndexRepository, never()).delete(any());
    }

    @Test
    void shouldReturnExhibitionDTOWhenOldIdExists() {
        // Given
        var oldId = 100;
        var exhibition = new Exhibition();
        exhibition.setId(1);
        exhibition.getOldIds().add(oldId);

        var expectedDTO = new ExhibitionDTO();
        expectedDTO.setId(1);
        expectedDTO.setOldId(oldId);

        when(exhibitionRepository.findExhibitionByOldIdsContains(oldId)).thenReturn(
            Optional.of(exhibition));

        // When
        var response = exhibitionService.readExhibitionByOldId(oldId);

        // Then
        assertNotNull(response);
        assertEquals(expectedDTO.getId(), response.getId());
        assertEquals(expectedDTO.getOldId(), response.getOldId());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenOldIdDoesNotExist() {
        // Given
        var oldId = 200;
        when(exhibitionRepository.findExhibitionByOldIdsContains(oldId)).thenReturn(
            Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> exhibitionService.readExhibitionByOldId(oldId));

        assertEquals("Exhibition with old ID " + oldId + " does not exist.",
            exception.getMessage());
    }

    @Test
    void shouldReindexVolatileExhibitionInformationWhenExhibitionExists() {
        // Given
        var exhibitionId = 123;
        var eventIndex = new EventIndex();
        eventIndex.setEventType(EventType.EXHIBITION);

        var institutionIds = Set.of(1, 2, 3);

        when(eventIndexRepository.findByDatabaseId(exhibitionId))
            .thenReturn(Optional.of(eventIndex));
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(exhibitionId))
            .thenReturn(institutionIds);

        // When
        exhibitionService.reindexVolatileExhibitionInformation(exhibitionId);

        // Then
        institutionIds.stream().toList().forEach(institutionId -> assertFalse(
            eventIndex.getRelatedInstitutionIds().contains(institutionId)));
        verify(eventIndexRepository).save(eventIndex);
    }

    @Test
    void shouldNotReindexWhenExhibitionDoesNotExist() {
        // Given
        var exhibitionId = 456;
        when(eventIndexRepository.findByDatabaseId(exhibitionId))
            .thenReturn(Optional.empty());

        // When
        exhibitionService.reindexVolatileExhibitionInformation(exhibitionId);

        // Then
        verify(eventRepository, never()).findInstitutionIdsByEventIdAndAuthorContribution(any());
        verify(eventIndexRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyInstitutionList() {
        // Given
        var exhibitionId = 789;
        var eventIndex = new EventIndex();
        eventIndex.setEventType(EventType.EXHIBITION);

        when(eventIndexRepository.findByDatabaseId(exhibitionId))
            .thenReturn(Optional.of(eventIndex));
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(exhibitionId))
            .thenReturn(Collections.emptySet());

        // When
        exhibitionService.reindexVolatileExhibitionInformation(exhibitionId);

        // Then
        assertTrue(eventIndex.getRelatedInstitutionIds().isEmpty());
        verify(eventIndexRepository).save(eventIndex);
    }

    @Test
    void shouldUpdateFieldsAndSaveWhenReindexingVolatileExhibitionInformation() {
        // Given
        var exhibitionId = 3;
        var eventIndex = mock(EventIndex.class);
        when(eventIndex.getDatabaseId()).thenReturn(exhibitionId);
        when(eventIndex.getEventType()).thenReturn(EventType.EXHIBITION);
        when(eventIndexRepository.findByDatabaseId(exhibitionId))
            .thenReturn(Optional.of(eventIndex));
        var institutionIds = Set.of(100, 200, 300);
        var classifiedBy = List.of(1, 2);
        when(eventRepository.findInstitutionIdsByEventIdAndAuthorContribution(
            exhibitionId)).thenReturn(institutionIds);
        when(commissionRepository.findCommissionsThatClassifiedEvent(exhibitionId)).thenReturn(
            classifiedBy);

        // When
        exhibitionService.reindexVolatileExhibitionInformation(exhibitionId);

        // Then
        verify(eventIndex).getRelatedInstitutionIds();
        verify(eventIndex).setClassifiedBy(classifiedBy);
        verify(eventIndexRepository).save(eventIndex);
    }

    @Test
    void shouldReturnRawExhibition() {
        // Given
        var entityId = 123;
        var expected = new Exhibition();
        expected.setId(entityId);
        when(exhibitionRepository.findRaw(entityId)).thenReturn(Optional.of(expected));

        // When
        var actual = exhibitionService.findRaw(entityId);

        // Then
        assertEquals(expected, actual);
        verify(exhibitionRepository).findRaw(entityId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenExhibitionDoesNotExist() {
        // Given
        var entityId = 123;
        when(exhibitionRepository.findRaw(entityId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> exhibitionService.findRaw(entityId));

        assertEquals("Exhibition with given ID does not exist.", exception.getMessage());
        verify(exhibitionRepository).findRaw(entityId);
    }
}
