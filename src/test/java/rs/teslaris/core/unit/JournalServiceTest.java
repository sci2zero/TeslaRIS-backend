package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import rs.teslaris.core.dto.document.JournalBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublicationSeriesDTO;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.document.PublicationSeriesRepository;
import rs.teslaris.core.service.impl.document.JournalServiceImpl;
import rs.teslaris.core.service.impl.document.cruddelegate.JournalJPAServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;

@SpringBootTest
public class JournalServiceTest {

    @Mock
    private JournalJPAServiceImpl journalJPAService;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private MultilingualContentService multilingualContentService;

    @Mock
    private LanguageTagService languageTagService;

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
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

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
        verify(emailUtil, times(1)).notifyInstitutionalEditor(1, "journal");
    }

    @Test
    public void shouldUpdateJournalWhenProvidedWithValidData() {
        // given
        var journalId = 1;
        var journalDTO = new PublicationSeriesDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        var journal = new Journal();
        var journalIndex = new JournalIndex();
        journalIndex.setDatabaseId(journalId);

        when(journalJPAService.findOne(journalId)).thenReturn(journal);
        when(journalJPAService.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.of(journalIndex));

        // when
        journalService.updateJournal(journalDTO, journalId);

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
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        var journal = new Journal();

        when(journalJPAService.findOne(journalId)).thenReturn(journal);
        when(journalJPAService.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.empty());

        // when
        journalService.updateJournal(journalDTO, journalId);

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

    @Test
    public void shouldFindJournalWhenSearchingWithSimpleQuery() {
        // given
        var tokens = Arrays.asList("DEF CON", "DEFCON");
        var pageable = PageRequest.of(0, 10);

        when(searchService.runQuery(any(), any(), any(), any())).thenReturn(
            new PageImpl<>(List.of(new JournalIndex(), new JournalIndex())));

        // when
        var result = journalService.searchJournals(new ArrayList<>(tokens), pageable);

        // then
        assertEquals(result.getTotalElements(), 2L);
    }

    @Test
    public void shouldReindexJournals() {
        // Given
        var journal1 = new Journal();
        var journal2 = new Journal();
        var journal3 = new Journal();
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
    }

    @Test
    public void shouldReturnJournalWhenOldIdExists() {
        // Given
        var journalId = 123;
        var expectedJournal = new Journal();
        when(journalRepository.findJournalByOldId(journalId)).thenReturn(
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
        when(journalRepository.findJournalByOldId(journalId)).thenReturn(Optional.empty());

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
}
