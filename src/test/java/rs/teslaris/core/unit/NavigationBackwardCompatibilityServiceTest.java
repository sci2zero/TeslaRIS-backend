package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.model.document.DocumentFile;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.institution.OrganisationUnit;
import rs.teslaris.core.model.person.Person;
import rs.teslaris.core.repository.document.BookSeriesRepository;
import rs.teslaris.core.repository.document.DocumentFileRepository;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.repository.document.JournalRepository;
import rs.teslaris.core.repository.person.OrganisationUnitRepository;
import rs.teslaris.core.repository.person.PersonRepository;
import rs.teslaris.core.service.impl.commontypes.NavigationBackwardCompatibilityServiceImpl;

@SpringBootTest
public class NavigationBackwardCompatibilityServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private OrganisationUnitRepository organisationUnitRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private JournalRepository journalRepository;

    @Mock
    private BookSeriesRepository bookSeriesRepository;

    @Mock
    private DocumentFileRepository documentFileRepository;

    @InjectMocks
    private NavigationBackwardCompatibilityServiceImpl service;


    @Test
    public void shouldReturnJournalPublicationWhenOldIdMatches() {
        // given
        var jp = new JournalPublication();
        jp.setId(123);
        when(documentRepository.findDocumentByOldId(1)).thenReturn(Optional.of(jp));

        // when
        var result = service.readResourceByOldId(1, "sourceA", "en");

        // then
        assertNotNull(result);
        assertEquals("JOURNAL_PUBLICATION", result.a);
        assertEquals(123, result.b);
    }

    @Test
    public void shouldReturnJournalWhenNoDocumentFoundButJournalMatches() {
        // given
        var journal = new Journal();
        journal.setId(555);
        when(documentRepository.findDocumentByOldId(1)).thenReturn(Optional.empty());
        when(journalRepository.findJournalByOldId(1)).thenReturn(Optional.of(journal));

        // when
        var result = service.readResourceByOldId(1, "sourceB", "sr");

        // then
        assertNotNull(result);
        assertEquals("JOURNAL", result.a);
        assertEquals(555, result.b);
    }

    @Test
    public void shouldReturnPersonWhenNoDocumentOrJournalFoundButPersonMatches() {
        // given
        var person = new Person();
        person.setId(777);
        when(documentRepository.findDocumentByOldId(1)).thenReturn(Optional.empty());
        when(journalRepository.findJournalByOldId(1)).thenReturn(Optional.empty());
        when(bookSeriesRepository.findBookSeriesByOldId(1)).thenReturn(Optional.empty());
        when(personRepository.findPersonByOldId(1)).thenReturn(Optional.of(person));

        // when
        var result = service.readResourceByOldId(1, "sourceC", "de");

        // then
        assertNotNull(result);
        assertEquals("PERSON", result.a);
        assertEquals(777, result.b);
    }

    @Test
    public void shouldReturnOrganisationUnitWhenOnlyItMatches() {
        // given
        var org = new OrganisationUnit();
        org.setId(888);
        when(documentRepository.findDocumentByOldId(1)).thenReturn(Optional.empty());
        when(journalRepository.findJournalByOldId(1)).thenReturn(Optional.empty());
        when(bookSeriesRepository.findBookSeriesByOldId(1)).thenReturn(Optional.empty());
        when(personRepository.findPersonByOldId(1)).thenReturn(Optional.empty());
        when(organisationUnitRepository.findOrganisationUnitByOldId(1)).thenReturn(
            Optional.of(org));

        // when
        var result = service.readResourceByOldId(1, "sourceD", "fr");

        // then
        assertNotNull(result);
        assertEquals("ORGANISATION_UNIT", result.a);
        assertEquals(888, result.b);
    }

    @Test
    public void shouldReturnNotFoundWhenNoEntityMatchesOldId() {
        // given
        when(documentRepository.findDocumentByOldId(1)).thenReturn(Optional.empty());
        when(journalRepository.findJournalByOldId(1)).thenReturn(Optional.empty());
        when(bookSeriesRepository.findBookSeriesByOldId(1)).thenReturn(Optional.empty());
        when(personRepository.findPersonByOldId(1)).thenReturn(Optional.empty());
        when(organisationUnitRepository.findOrganisationUnitByOldId(1)).thenReturn(
            Optional.empty());

        // when
        var result = service.readResourceByOldId(1, "sourceX", "it");

        // then
        assertNotNull(result);
        assertEquals("NOT_FOUND", result.a);
        assertEquals(-1, result.b);
    }

    @Test
    public void shouldReturnDocumentFileWhenLegacyFilenameMatches() {
        // given
        var file = new DocumentFile();
        file.setId(321);
        file.setServerFilename("old_abc.pdf");
        file.setFilename("file.pdf");
        when(documentFileRepository.findDocumentFileByLegacyFilename("old_abc.pdf")).thenReturn(
            Optional.of(file));

        // when
        var result = service.readDocumentFileByOldId("old_abc.pdf", "sourceY", "en");

        // then
        assertNotNull(result);
        assertEquals("old_abc.pdf", result.a);
        assertEquals("file.pdf", result.b);
    }

    @Test
    public void shouldReturnNotFoundForDocumentFileWhenLegacyFilenameDoesNotMatch() {
        // given
        when(
            documentFileRepository.findDocumentFileByLegacyFilename("unknown_file.pdf")).thenReturn(
            Optional.empty());

        // when
        var result = service.readDocumentFileByOldId("unknown_file.pdf", "sourceZ", "en");

        // then
        assertNotNull(result);
        assertEquals("NOT_FOUND", result.a);
        assertEquals("", result.b);
    }
}
