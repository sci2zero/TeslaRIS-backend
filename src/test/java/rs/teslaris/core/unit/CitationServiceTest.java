package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rs.teslaris.core.dto.document.CitationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.model.document.Journal;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.JournalPublicationRepository;
import rs.teslaris.core.repository.document.ProceedingsPublicationRepository;
import rs.teslaris.core.service.impl.document.CitationServiceImpl;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class CitationServiceTest {

    @Mock
    private JournalPublicationRepository journalPublicationRepository;

    @Mock
    private ProceedingsPublicationRepository proceedingsPublicationRepository;

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @InjectMocks
    private CitationServiceImpl citationService;

    private DocumentPublicationIndex mockDocumentIndex() {
        var index = new DocumentPublicationIndex();
        index.setType("JOURNAL_PUBLICATION");
        index.setTitleSr("Test Title");
        index.setYear(2024);
        index.setAuthorNames("John Doe; Jane Smith");
        index.setDatabaseId(1);
        return index;
    }

    @Test
    void shouldCraftCitationsFromDocumentIndex() {
        // Given
        var index = mockDocumentIndex();
        var languageCode = "EN";
        var publication = new JournalPublication();
        var journal = new Journal();
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag(languageCode);
        journal.setTitle(Set.of(new MultiLingualContent(languageTag, "Dummy content", 1)));
        publication.setJournal(journal);

        when(journalPublicationRepository.findById(1)).thenReturn(Optional.of(publication));

        // When
        CitationResponseDTO result = citationService.craftCitations(index, languageCode);

        // Then
        assertNotNull(result);
        assertNotNull(result.getApa());
        assertNotNull(result.getMla());
        assertNotNull(result.getChicago());
        assertNotNull(result.getHarvard());
        assertNotNull(result.getVancouver());
    }

    @Test
    void shouldCraftCitationsFromDocumentId() {
        // Given
        var documentId = 1;
        var languageCode = "EN";
        var index = mockDocumentIndex();
        var publication = new JournalPublication();
        var journal = new Journal();
        var languageTag = new LanguageTag();
        languageTag.setLanguageTag(languageCode);
        journal.setTitle(Set.of(new MultiLingualContent(languageTag, "Dummy content", 1)));
        publication.setJournal(journal);

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.of(index));
        when(journalPublicationRepository.findById(1)).thenReturn(Optional.of(publication));

        // When
        CitationResponseDTO result = citationService.craftCitations(documentId, languageCode);

        // Then
        assertNotNull(result);
        verify(documentPublicationIndexRepository).findDocumentPublicationIndexByDatabaseId(
            documentId);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenDocumentIdNotFound() {
        // Given
        int documentId = 99;
        String languageCode = "en";

        when(
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(documentId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(NotFoundException.class,
            () -> citationService.craftCitations(documentId, languageCode));
    }
}
