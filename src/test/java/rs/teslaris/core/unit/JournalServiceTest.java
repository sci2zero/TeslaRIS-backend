package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
import rs.teslaris.core.dto.document.JournalDTO;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.service.impl.document.JournalServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.MultilingualContentService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.person.PersonContributionService;
import rs.teslaris.core.util.email.EmailUtil;
import rs.teslaris.core.util.exceptionhandling.exception.JournalReferenceConstraintViolationException;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@SpringBootTest
public class JournalServiceTest {

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

    @InjectMocks
    private JournalServiceImpl journalService;


    @Test
    public void shouldReturnJournalWhenItExists() {
        // given
        var expected = new Journal();
        when(journalRepository.findById(1)).thenReturn(Optional.of(expected));

        // when
        var result = journalService.findOne(1);

        // then
        assertEquals(expected, result);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenJournalDoesNotExist() {
        // given
        when(journalRepository.findById(1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class, () -> journalService.findOne(1));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldCreateJournalWhenProvidedWithValidData() {
        // given
        var journalDTO = new JournalDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        when(journalRepository.save(any())).thenReturn(new Journal());

        // when
        var savedJournal = journalService.createJournal(journalDTO);

        // then
        assertNotNull(savedJournal);
        verify(journalRepository, times(1)).save(any());
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
        when(journalRepository.save(any())).thenReturn(journal);

        // when
        var savedJournal = journalService.createJournal(journalDTO);

        // then
        assertNotNull(savedJournal);
        verify(journalRepository, times(1)).save(any());
        verify(emailUtil, times(1)).notifyInstitutionalEditor(1, "journal");
    }

    @Test
    public void shouldUpdateJournalWhenProvidedWithValidData() {
        // given
        var journalId = 1;
        var journalDTO = new JournalDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        var journal = new Journal();
        journal.setLanguages(new HashSet<>());
        var journalIndex = new JournalIndex();
        journalIndex.setDatabaseId(journalId);

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(journal));
        when(journalRepository.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.of(journalIndex));

        // when
        journalService.updateJournal(journalDTO, journalId);

        // then
        verify(journalRepository, times(1)).save(any());
    }

    @Test
    public void shouldUpdateJournalWhenProvidedWithValidDataNonIndexed() {
        // given
        var journalId = 1;
        var journalDTO = new JournalDTO();
        journalDTO.setTitle(new ArrayList<>());
        journalDTO.setNameAbbreviation(new ArrayList<>());
        journalDTO.setEissn("eISSN");
        journalDTO.setPrintISSN("printISSN");
        journalDTO.setContributions(new ArrayList<>());
        journalDTO.setLanguageTagIds(new ArrayList<>());

        var journal = new Journal();
        journal.setLanguages(new HashSet<>());

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(journal));
        when(journalRepository.save(any())).thenReturn(new Journal());
        when(journalIndexRepository.findJournalIndexByDatabaseId(journalId)).thenReturn(
            Optional.empty());

        // when
        journalService.updateJournal(journalDTO, journalId);

        // then
        verify(journalRepository, times(1)).save(any());
    }

    @Test
    public void shouldDeleteJournalWhenNotUsed() {
        // given
        var journalId = 1;
        var journalToDelete = new Journal();

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(journalToDelete));
        when(journalRepository.hasPublication(journalId)).thenReturn(false);
        when(journalRepository.hasProceedings(journalId)).thenReturn(false);

        // when
        journalService.deleteJournal(journalId);

        // then
        verify(journalRepository, times(1)).save(journalToDelete);
        verify(journalRepository, never()).delete(any());
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true", "true, true"})
    public void shouldNotDeleteJournalIfInUsage(Boolean hasPublication, Boolean hasProceedings) {
        // given
        var journalId = 1;
        var journalToDelete = new Journal();

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(journalToDelete));
        when(journalRepository.hasPublication(journalId)).thenReturn(hasPublication);
        when(journalRepository.hasProceedings(journalId)).thenReturn(hasProceedings);

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
        journal1.setTitle(new HashSet<>());
        journal1.setNameAbbreviation(new HashSet<>());
        journal1.setEISSN("eISSN1");
        journal1.setPrintISSN("printISSN1");
        journal1.setContributions(new HashSet<>());
        journal1.setLanguages(new HashSet<>());
        var journal2 = new Journal();
        journal2.setTitle(new HashSet<>());
        journal2.setNameAbbreviation(new HashSet<>());
        journal2.setEISSN("eISSN2");
        journal2.setPrintISSN("printISSN2");
        journal2.setContributions(new HashSet<>());
        journal2.setLanguages(new HashSet<>());

        when(journalRepository.findAll(pageable)).thenReturn(
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
        journal.setTitle(new HashSet<>());
        journal.setNameAbbreviation(new HashSet<>());
        journal.setEISSN("eISSN1");
        journal.setPrintISSN("printISSN1");
        journal.setContributions(new HashSet<>());
        journal.setLanguages(new HashSet<>());

        when(journalRepository.findById(journalId)).thenReturn(Optional.of(journal));

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
}
