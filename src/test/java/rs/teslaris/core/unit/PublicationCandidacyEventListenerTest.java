package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rs.teslaris.core.applicationevent.PublicationCandidacyChangedEvent;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.revision.PublicationCandidacyUpdater;
import rs.teslaris.core.service.impl.eventlistener.PublicationCandidacyEventListener;

public class PublicationCandidacyEventListenerTest {

    private PublicationCandidacyUpdater personUpdater;

    private PublicationCandidacyUpdater eventUpdater;

    private PublicationCandidacyEventListener listener;


    @BeforeEach
    void setUp() {
        personUpdater = mock(PublicationCandidacyUpdater.class);
        when(personUpdater.entityTypes()).thenReturn(Set.of(EntityType.PERSON.name()));

        eventUpdater = mock(PublicationCandidacyUpdater.class);
        when(eventUpdater.entityTypes()).thenReturn(
            Set.of(EntityType.CONFERENCE.name(), EntityType.EXHIBITION.name()));

        listener = new PublicationCandidacyEventListener(List.of(personUpdater, eventUpdater));
    }

    @Test
    void shouldRouteEventToUpdaterOfEntityType() {
        // When
        listener.handlePublicationCandidacyChanged(
            new PublicationCandidacyChangedEvent(EntityType.EXHIBITION.name(), 5, false));

        // Then
        verify(eventUpdater).apply(EntityType.EXHIBITION.name(), 5, false);
        verify(personUpdater, never()).apply(anyString(), anyInt(), anyBoolean());
    }

    @Test
    void shouldIgnoreEntityTypeWithoutUpdater() {
        // When
        listener.handlePublicationCandidacyChanged(
            new PublicationCandidacyChangedEvent(EntityType.PRIZE.name(), 5, false));

        // Then
        verify(personUpdater, never()).apply(anyString(), anyInt(), anyBoolean());
        verify(eventUpdater, never()).apply(anyString(), anyInt(), anyBoolean());
    }

    @Test
    void shouldNotPropagateUpdaterFailure() {
        // Given
        doThrow(new IllegalStateException("index down")).when(personUpdater)
            .apply(EntityType.PERSON.name(), 3, true);

        // When / Then
        assertDoesNotThrow(() -> listener.handlePublicationCandidacyChanged(
            new PublicationCandidacyChangedEvent(EntityType.PERSON.name(), 3, true)));
    }

    @Test
    void shouldRejectTwoUpdatersForSameEntityType() {
        // Given
        var duplicate = mock(PublicationCandidacyUpdater.class);
        when(duplicate.entityTypes()).thenReturn(Set.of(EntityType.PERSON.name()));

        // When / Then
        assertThrows(IllegalStateException.class,
            () -> new PublicationCandidacyEventListener(List.of(personUpdater, duplicate)));
    }
}
