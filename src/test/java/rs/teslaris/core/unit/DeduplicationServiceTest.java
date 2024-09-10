package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationBlacklist;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationSuggestion;
import rs.teslaris.core.model.document.Monograph;
import rs.teslaris.core.repository.commontypes.DocumentDeduplicationBlacklistRepository;
import rs.teslaris.core.repository.commontypes.DocumentDeduplicationSuggestionRepository;
import rs.teslaris.core.service.impl.document.DeduplicationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;

@SpringBootTest
public class DeduplicationServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private SearchService<DocumentPublicationIndex> searchService;

    @Mock
    private DocumentDeduplicationSuggestionRepository deduplicationSuggestionRepository;

    @Mock
    private DocumentPublicationService documentPublicationService;

    @Mock
    private DocumentDeduplicationBlacklistRepository documentDeduplicationBlacklistRepository;

    @InjectMocks
    private DeduplicationServiceImpl deduplicationService;


    @Test
    public void shouldDeleteDocumentSuggestionWhenSuggestionExists() {
        // given
        var suggestion = new DocumentDeduplicationSuggestion();
        when(deduplicationSuggestionRepository.findById(1)).thenReturn(Optional.of(suggestion));

        // when
        deduplicationService.deleteDocumentSuggestion(1);

        // then
        verify(deduplicationSuggestionRepository).delete(suggestion);
    }

    @Test
    public void shouldFlagDocumentAsNotDuplicateWhenSuggestionExistsAndNotInBlacklist() {
        // given
        var suggestion = new DocumentDeduplicationSuggestion();
        var leftDocument = new Monograph();
        var rightDocument = new Monograph();
        suggestion.setLeftDocument(leftDocument);
        suggestion.setRightDocument(rightDocument);

        when(deduplicationSuggestionRepository.findById(1)).thenReturn(Optional.of(suggestion));
        when(documentDeduplicationBlacklistRepository.findByLeftDocumentIdAndRightDocumentId(
            leftDocument.getId(), rightDocument.getId())).thenReturn(Optional.empty());

        // when
        deduplicationService.flagDocumentAsNotDuplicate(1);

        // then
        verify(documentDeduplicationBlacklistRepository).save(
            any(DocumentDeduplicationBlacklist.class));
        verify(deduplicationSuggestionRepository).delete(suggestion);
    }

    @Test
    public void shouldReturnPageOfSuggestionsWhenSuggestionsExist() {
        // given
        var pageable = PageRequest.of(0, 10);
        var suggestion = new DocumentDeduplicationSuggestion();
        suggestion.setLeftDocument(new Monograph());
        suggestion.setRightDocument(new Monograph());
        var suggestions = List.of(suggestion);
        var page = new PageImpl<>(suggestions, pageable, suggestions.size());

        when(deduplicationSuggestionRepository.findAll(pageable)).thenReturn(page);

        // when
        var result = deduplicationService.getDeduplicationSuggestions(pageable);

        // then
        assertEquals(suggestions.size(), result.getTotalElements());
    }

    @Test
    public void shouldReturnFalseWhenDeduplicationLockIsTrue() {
        // given
        ReflectionTestUtils.setField(deduplicationService, "deduplicationLock", true);

        // when
        boolean result = deduplicationService.startDocumentDeduplicationProcessBeforeSchedule(1);

        // then
        assertFalse(result);
    }
}
