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
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationBlacklist;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.deduplication.DocumentDeduplicationBlacklistRepository;
import rs.teslaris.core.indexrepository.deduplication.DocumentDeduplicationSuggestionRepository;
import rs.teslaris.core.service.impl.document.DeduplicationServiceImpl;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.user.UserService;

@SpringBootTest
public class DeduplicationServiceTest {

    @Mock
    private DocumentPublicationIndexRepository documentPublicationIndexRepository;

    @Mock
    private JournalIndexRepository journalIndexRepository;

    @Mock
    private EventIndexRepository eventIndexRepository;

    @Mock
    private PersonIndexRepository personIndexRepository;

    @Mock
    private DocumentDeduplicationSuggestionRepository deduplicationSuggestionRepository;

    @Mock
    private DocumentDeduplicationBlacklistRepository documentDeduplicationBlacklistRepository;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SearchService<DocumentPublicationIndex> documentSearchService;

    @Mock
    private SearchService<JournalIndex> journalSearchService;

    @Mock
    private SearchService<EventIndex> eventSearchService;

    @Mock
    private SearchService<PersonIndex> personSearchService;

    @InjectMocks
    private DeduplicationServiceImpl deduplicationService;


    @Test
    public void shouldDeleteSuggestionWhenSuggestionExists() {
        // given
        var suggestion = new DeduplicationSuggestion();
        when(deduplicationSuggestionRepository.findById("testId")).thenReturn(
            Optional.of(suggestion));

        // when
        deduplicationService.deleteSuggestion("testId");

        // then
        verify(deduplicationSuggestionRepository).delete(suggestion);
    }

    @Test
    public void shouldFlagDocumentAsNotDuplicateWhenSuggestionExistsAndNotInBlacklist() {
        // given
        var suggestion = new DeduplicationSuggestion();
        suggestion.setLeftEntityId(1);
        suggestion.setRightEntityId(2);
        suggestion.setEntityType(EntityType.PUBLICATION);

        when(deduplicationSuggestionRepository.findById("testId")).thenReturn(
            Optional.of(suggestion));
        when(documentDeduplicationBlacklistRepository.findByEntityIdsAndEntityType(1, 2,
            EntityType.PUBLICATION.name())).thenReturn(Optional.empty());

        // when
        deduplicationService.flagAsNotDuplicate("testId");

        // then
        verify(documentDeduplicationBlacklistRepository).save(any(DeduplicationBlacklist.class));
        verify(deduplicationSuggestionRepository).delete(suggestion);
    }

    @Test
    public void shouldReturnPageOfSuggestionsWhenSuggestionsExist() {
        // given
        var pageable = PageRequest.of(0, 10);
        var suggestion = new DeduplicationSuggestion();
        suggestion.setLeftEntityId(1);
        suggestion.setRightEntityId(2);
        var suggestions = List.of(suggestion);
        var page = new PageImpl<>(suggestions, pageable, suggestions.size());

        when(deduplicationSuggestionRepository.findByEntityType(EntityType.PUBLICATION.name(),
            pageable)).thenReturn(page);

        // when
        var result =
            deduplicationService.getDeduplicationSuggestions(pageable, EntityType.PUBLICATION);

        // then
        assertEquals(suggestions.size(), result.getTotalElements());
    }

    @Test
    public void shouldReturnFalseWhenDeduplicationLockIsTrue() {
        // given
        ReflectionTestUtils.setField(deduplicationService, "deduplicationLock", true);

        // when
        boolean result = deduplicationService.canPerformDeduplication();

        // then
        assertFalse(result);
    }

    @Test
    public void shouldDeleteSuggestionsWhenSuggestionsExist() {
        // given
        var deletedEntityId = 1;
        var entityType = EntityType.PUBLICATION;

        var suggestion = new DeduplicationSuggestion();
        suggestion.setLeftEntityId(deletedEntityId);
        suggestion.setRightEntityId(2);

        var suggestions = List.of(suggestion);

        // Mock repository behavior
        when(deduplicationSuggestionRepository.findByEntityIdAndEntityType(deletedEntityId,
            entityType.name()))
            .thenReturn(suggestions);

        // when
        deduplicationService.deleteSuggestion(deletedEntityId, entityType);

        // then
        verify(deduplicationSuggestionRepository).deleteAll(suggestions);
    }
}
