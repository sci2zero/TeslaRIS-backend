package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.commontypes.Language;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.repository.institution.CommissionRepository;
import rs.teslaris.core.service.impl.document.JournalServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.IndexBulkUpdateService;
import rs.teslaris.core.service.interfaces.commontypes.LanguageService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class JournalServiceTest {

    @Mock
    private JournalJPAServiceImpl journalJPAService;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private LanguageService languageService;

    @Mock
    private PersonContributionService personContributionService;

    @Mock
    private JournalIndexRepository journalIndexRepository;

    @Mock
    private SearchService<JournalIndex> searchService;

    @Mock
    private EmailUtil emailUtil;

    @Mock
    private PublicationSeriesRepository publicationSeriesRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private IndexBulkUpdateService indexBulkUpdateService;

    @Mock
    private CommissionRepository commissionRepository;

    @InjectMocks
    private JournalServiceImpl journalService;


    @Test
    public void shouldReturnJournalWhenItExists() {
        // given
        var expected = new Journal();
        when(journalJPAService.findOne(1)).thenReturn(expected);

        // when
        var result = journalService.findJournalById(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldCreateJournalWhenProvidedWithValidData() {
        // given
        var journalDTO = new PublicationSeriesDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("8765-4322");
        journalDTO.setPrintISSN("8765-4322");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageIds(new ArrayList<>());

        when(journalJPAService.save(any())).thenReturn(new Journal());

        // when
        var savedJournal = journalService.createJournal(journalDTO, true);

        // then
        assertNotNull(savedJournal);
        verify(journalJPAService, times(1)).save(any());
    }

    @Test
    public void shouldCreateJournalBasicWhenProvidedWithValidData() {
        // given
        var journalDTO = new JournalBasicAdditionDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setEISSN("eISSN");
        journalDTO.setPrintISSN("printISSN");

        var journal = new Journal();
        journal.setId(1);
        when(journalJPAService.save(any())).thenReturn(journal);

        // when
        var savedJournal = journalService.createJournal(journalDTO);

        // then
        assertNotNull(savedJournal);
        verify(journalJPAService, times(1)).save(any());
        verify(commissionRepository).findCommissionsThatClassifiedJournal(any());
    }

    @Test
    public void shouldUpdateJournalWhenProvidedWithValidData() {
        // given
        var journalId = 1;
        var journalDTO = new PublicationSeriesDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("1234-5678");
        journalDTO.setPrintISSN("1234-5678");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageIds(new ArrayList<>());

        var journal = new Journal();
        var journalIndex = new JournalIndex();
        journalIndex.setDatabaseId(journalId);

        when(journalJPAService.findOne(journalId)).thenReturn(journal);
        when(journalJPAService.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.of(journalIndex));

        // when
        journalService.updateJournal(journalId, journalDTO);

        // then
        verify(journalJPAService, times(1)).save(any());
    }

    @Test
    public void shouldUpdateJournalWhenProvidedWithValidDataNonIndexed() {
        // given
        var journalId = 1;
        var journalDTO = new PublicationSeriesDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("8765-4321");
        journalDTO.setPrintISSN("8765-4321");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageIds(new ArrayList<>());

        var journal = new Journal();

        when(journalJPAService.findOne(journalId)).thenReturn(journal);
        when(journalJPAService.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.empty());

        // when
        journalService.updateJournal(journalId, journalDTO);

        // then
        verify(journalJPAService, times(1)).save(any());
    }

    @Test
    public void shouldDeleteJournalWhenNotUsed() {
        // given
        var journalId = 1;
        var journalToDelete = new Journal();

        when(journalJPAService.findOne(journalId)).thenReturn(journalToDelete);
        when(journalRepository.hasPublication(journalId)).thenReturn(false);
        when(publicationSeriesRepository.hasProceedings(journalId)).thenReturn(false);

        // when
        journalService.deleteJournal(journalId);

        // then
        verify(journalJPAService, times(1)).delete(any());
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true", "true, true"})
    public void shouldNotDeleteJournalIfInUsage(Boolean hasPublication, Boolean hasProceedings) {
        // given
        var journalId = 1;
        var journalToDelete = new Journal();

        when(journalJPAService.findOne(journalId)).thenReturn(journalToDelete);
        when(journalRepository.hasPublication(journalId)).thenReturn(hasPublication);
        when(publicationSeriesRepository.hasProceedings(journalId)).thenReturn(hasProceedings);

        // when
        assertThrows(JournalReferenceConstraintViolationException.class,
            () -> journalService.deleteJournal(journalId));

        // then (JournalReferenceConstraintViolationException should be thrown)
    }

    @Test
    public void shouldReadAllJournals() {
        // given
        var pageable = Pageable.ofSize(5);
        var journal1 = new Journal();
        journal1.setEISSN("eISSN1");
        journal1.setPrintISSN("printISSN1");
        var journal2 = new Journal();
        journal2.setEISSN("eISSN2");
        journal2.setPrintISSN("printISSN2");

        when(journalJPAService.findAll(pageable)).thenReturn(
            new PageImpl<>(List.of(journal1, journal2)));

        // when
        var response = journalService.readAllJournals(pageable);

        // then
        assertNotNull(response);
    }

    @Test
    public void shouldReadJournal() {
        // given
        var journalId = 1;
        var journal = new Journal();
        journal.setEISSN("eISSN1");
        journal.setPrintISSN("printISSN1");

        when(journalJPAService.findOne(journalId)).thenReturn(journal);

        // when
        var response = journalService.readJournal(journalId);

        // then
        assertNotNull(response);
        assertEquals(response.getEissn(), journal.getEISSN());
        assertEquals(response.getPrintISSN(), journal.getPrintISSN());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void shouldFindJournalWhenSearchingWithSimpleQuery(Integer institutionId) {
        // given
        var tokens = Arrays.asList("DEF CON", "DEFCON");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new JournalIndex(), new JournalIndex())));

        // when
        var result =
            journalService.searchJournals(new ArrayList<>(tokens), pageable, institutionId,
                institutionId + 1);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexJournals() {
        // Given
        var journal1 = new Journal() {{
            setId(1);
        }};
        var journal2 = new Journal() {{
            setId(2);
        }};
        var journal3 = new Journal() {{
            setId(3);
        }};
        var journals = Arrays.asList(journal1, journal2, journal3);
        var page1 = new PageImpl<>(journals.subList(0, 2), PageRequest.of(0, 10), journals.size());
        var page2 = new PageImpl<>(journals.subList(2, 3), PageRequest.of(1, 10), journals.size());

        when(journalJPAService.findAll(any(PageRequest.class))).thenReturn(page1, page2);

        // When
        journalService.reindexJournals();

        // Then
        verify(journalIndexRepository, times(1)).deleteAll();
        verify(journalJPAService, atLeastOnce()).findAll(any(PageRequest.class));
        verify(journalIndexRepository, atLeastOnce()).save(any(JournalIndex.class));
        verify(commissionRepository, times(2)).findCommissionsThatClassifiedJournal(anyInt());
    }

    @Test
    public void shouldReturnJournalWhenOldIdExists() {
        // Given
        var journalId = 123;
        var expectedJournal = new Journal();
        when(journalRepository.findByOldIdsContains(journalId)).thenReturn(
            Optional.of(expectedJournal));

        // When
        var actualJournal = journalService.findJournalByOldId(journalId);

        // Then
        assertEquals(expectedJournal, actualJournal);
    }

    @Test
    public void shouldReturnNullWhenJournalDoesNotExist() {
        // Given
        var journalId = 123;
        when(journalRepository.findByOldIdsContains(journalId)).thenReturn(Optional.empty());

        // When
        var actualJournal = journalService.findJournalByOldId(journalId);

        // Then
        assertNull(actualJournal);
    }

    @Test
    public void shouldFindJournalByISSN() {
        // Given
        var journalIndex = new JournalIndex();
        journalIndex.setEISSN("12345");

        when(journalIndexRepository.findJournalIndexByeISSNOrPrintISSN(
            "12345", "6789")).thenReturn(Optional.of(journalIndex));

        // When
        var foundJournal = journalService.readJournalByIssn("12345", "6789");

        // Then
        assertEquals(journalIndex, foundJournal);
    }

    @Test
    public void shouldNotFindJournalByIssnWhenJournalDoesNotExist() {
        // Given
        when(journalIndexRepository.findJournalIndexByeISSNOrPrintISSN(
            "12345", "6789")).thenReturn(Optional.empty());

        // When
        var foundJournal = journalService.readJournalByIssn("12345", "6789");

        // Then
        assertNull(foundJournal);
    }

    @Test
    public void shouldFindJournalByIdentifiers() {
        // Given
        var journalIndex = new JournalIndex();
        journalIndex.setOpenAlexId("S1234");

        when(journalIndexRepository.findByAnyIdentifiers(
            "12345", "6789", "S1234")).thenReturn(Optional.of(journalIndex));

        // When
        var foundJournal = journalService.readJournalByIdentifiers("12345", "6789", "S1234");

        // Then
        assertEquals(journalIndex.getOpenAlexId(), foundJournal.getOpenAlexId());
    }

    @Test
    public void shouldNotFindJournalByIdentifiersWhenJournalDoesNotExist() {
        // Given
        when(journalIndexRepository.findByAnyIdentifiers(
            "12345", "6789", "S1234")).thenReturn(Optional.empty());

        // When
        var foundJournal = journalService.readJournalByIdentifiers("12345", "6789", "S1234");

        // Then
        assertNull(foundJournal);
    }

    @Test
    void shouldDeleteJournalAndRelatedEntitiesWhenJournalIndexIsPresent() {
        // Given
        var testJournalId = 1;
        var journalIndex = new JournalIndex();
        when(journalIndexRepository.findJournalIndexByDatabaseId(testJournalId)).thenReturn(
            Optional.of(journalIndex));

        // When
        journalService.forceDeleteJournal(testJournalId);

        // Then
        verify(journalRepository).deleteAllPublicationsInJournal(testJournalId);
        verify(publicationSeriesRepository).unbindProceedings(testJournalId);
        verify(journalJPAService).delete(testJournalId);
        verify(journalIndexRepository).delete(journalIndex);
        verify(documentPublicationIndexRepository).deleteByJournalIdAndType(testJournalId,
            DocumentPublicationType.JOURNAL_PUBLICATION.name());
    }

    @Test
    void shouldDeleteJournalAndRelatedEntitiesWhenJournalIndexIsAbsent() {
        // Given
        var testJournalId = 1;

        when(journalIndexRepository.findJournalIndexByDatabaseId(testJournalId)).thenReturn(
            Optional.empty());

        // When
        journalService.forceDeleteJournal(testJournalId);

        // Then
        verify(journalRepository).deleteAllPublicationsInJournal(testJournalId);
        verify(publicationSeriesRepository).unbindProceedings(testJournalId);
        verify(journalJPAService).delete(testJournalId);
        verify(journalIndexRepository, never()).delete(any());
        verify(documentPublicationIndexRepository).deleteByJournalIdAndType(testJournalId,
            DocumentPublicationType.JOURNAL_PUBLICATION.name());
    }

    @Test
    void shouldFindExistingPublicationSeriesWhenIssnSpecified() {
        // Given
        String[] line = {"dummy line"};
        var defaultLanguageTag = "en";
        var journalName = "Test Journal";
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";
        var issnSpecified = true;

        var expectedPublicationSeries = new Journal();
        expectedPublicationSeries.setPrintISSN("");
        expectedPublicationSeries.setEISSN("");
        when(publicationSeriesRepository.findPublicationSeriesByeISSNOrPrintISSN(eIssn, printIssn))
            .thenReturn(List.of(expectedPublicationSeries));

        // When
        var result = journalService.findOrCreatePublicationSeries(
            line, defaultLanguageTag, journalName, eIssn, printIssn, issnSpecified);

        // Then
        assertNotNull(result);
        verify(publicationSeriesRepository).findPublicationSeriesByeISSNOrPrintISSN(eIssn,
            printIssn);
        verify(journalRepository, never()).save(any());
    }

    @Test
    void shouldCreateNewPublicationSeriesWhenIssnNotSpecified() {
        // Given
        String[] line = {"dummy line"};
        var defaultLanguageTag = "en";
        var journalName = "New Journal";
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";
        var issnSpecified = false;

        var defaultLanguage = new Language();
        defaultLanguage.setId(1);
        when(languageService.findLanguageByCode(defaultLanguageTag)).thenReturn(
            defaultLanguage);

        when(searchService.runQuery(any(), any(), any(), anyString()))
            .thenReturn(new PageImpl<>(List.of()));
        when(journalJPAService.save(any())).thenReturn(new Journal() {{
            setId(1);
        }});

        // When
        var result = journalService.findOrCreatePublicationSeries(
            line, defaultLanguageTag, journalName, eIssn, printIssn, issnSpecified);

        // Then
        assertNotNull(result);
        verify(languageService).findLanguageByCode(defaultLanguageTag);
        verify(journalRepository, never()).save(any());
        verify(commissionRepository).findCommissionsThatClassifiedJournal(any());
    }

    @Test
    void shouldFindJournalByMatchingNameIgnoringCase() {
        // Given
        var journalName = "Test Journal";
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";

        var defaultLanguage = new Language();
        var potentialHit = new JournalIndex();
        potentialHit.setTitleOther("Test Journal|Dummy Journal");
        potentialHit.setDatabaseId(1);

        var potentialHits = List.of(potentialHit);
        when(searchService.runQuery(any(), any(), any(), anyString()))
            .thenReturn(new PageImpl<>(potentialHits));
        when(journalRepository.save(any())).thenReturn(new Journal());

        var existingJournal = new Journal();
        existingJournal.setEISSN(null);
        existingJournal.setPrintISSN(null);

        when(journalService.findJournalById(potentialHit.getDatabaseId())).thenReturn(
            existingJournal);

        // When
        var result =
            journalService.findJournalByJournalName(journalName, defaultLanguage, eIssn, printIssn);

        // Then
        assertNotNull(result);
        verify(journalRepository).save(existingJournal);
    }

    @Test
    void shouldCreateNewJournalWhenNoMatchFound() {
        // Given
        var journalName = "New Journal";
        var eIssn = "1234-5678";
        var printIssn = "8765-4321";

        var defaultLanguage = new Language();
        defaultLanguage.setId(1);
        when(searchService.runQuery(any(), any(), any(), anyString()))
            .thenReturn(new PageImpl<>(List.of()));
        when(journalJPAService.save(any())).thenReturn(new Journal() {{
            setId(1);
        }});

        // When
        var result =
            journalService.findJournalByJournalName(journalName, defaultLanguage, eIssn, printIssn);

        // Then
        assertNotNull(result);
        verify(searchService).runQuery(any(), any(), any(), anyString());
        verify(commissionRepository).findCommissionsThatClassifiedJournal(any());
    }

    @Test
    void shouldReindexJournalVolatileInformationWhenJournalExists() {
        // Given
        var journalId = 123;
        var journalIndex = new JournalIndex();
        var institutionIds = Set.of(1, 2, 3);

        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId))
            .thenReturn(Optional.of(journalIndex));
        when(journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(journalId))
            .thenReturn(institutionIds);

        // When
        journalService.reindexJournalVolatileInformation(journalId);

        // Then
        assertEquals(institutionIds.stream().toList(), journalIndex.getRelatedInstitutionIds());
        verify(journalIndexRepository).save(journalIndex);
    }

    @Test
    void shouldNotReindexWhenJournalDoesNotExist() {
        // Given
        var journalId = 456;
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId))
            .thenReturn(Optional.empty());

        // When
        journalService.reindexJournalVolatileInformation(journalId);

        // Then
        verify(journalRepository, never()).findInstitutionIdsByJournalIdAndAuthorContribution(
            any());
        verify(journalIndexRepository, never()).save(any());
    }

    @Test
    void shouldHandleEmptyInstitutionList() {
        // Given
        var journalId = 789;
        var journalIndex = new JournalIndex();
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId))
            .thenReturn(Optional.of(journalIndex));
        when(journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(journalId))
            .thenReturn(Collections.emptySet());

        // When
        journalService.reindexJournalVolatileInformation(journalId);

        // Then
        assertTrue(journalIndex.getRelatedInstitutionIds().isEmpty());
        verify(journalIndexRepository).save(journalIndex);
        verify(commissionRepository).findCommissionsThatClassifiedJournal(anyInt());
    }

    @Test
    void shouldUpdateRelatedInstitutionIdsWhenReindexJournalVolatileInformation() {
        // Given
        var journalId = 2;
        var journalIndex = mock(JournalIndex.class);
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId))
            .thenReturn(Optional.of(journalIndex));
        var institutionIds = Set.of(10, 20, 30);
        when(journalRepository.findInstitutionIdsByJournalIdAndAuthorContribution(
            journalId)).thenReturn(institutionIds);

        // When
        journalService.reindexJournalVolatileInformation(journalId);

        // Then
        verify(journalIndex).setRelatedInstitutionIds(institutionIds.stream().toList());
        verify(journalIndexRepository).save(journalIndex);
        verify(commissionRepository).findCommissionsThatClassifiedJournal(anyInt());
    }

    @Test
    void shouldReturnRawJournal() {
        // Given
        var entityId = 123;
        var expected = new Journal();
        expected.setId(entityId);
        when(journalRepository.findRaw(entityId)).thenReturn(Optional.of(expected));

        // When
        var actual = journalService.findRaw(entityId);

        // Then
        assertEquals(expected, actual);
        verify(journalRepository).findRaw(entityId);
    }

    @Test
    void shouldThrowsNotFoundExceptionWhenJournalDoesNotExist() {
        // Given
        var entityId = 123;
        when(journalRepository.findRaw(entityId)).thenReturn(Optional.empty());

        // When & Then
        var exception = assertThrows(NotFoundException.class,
            () -> journalService.findRaw(entityId));

        assertEquals("Journal with given ID does not exist.", exception.getMessage());
        verify(journalRepository).findRaw(entityId);
    }
}
