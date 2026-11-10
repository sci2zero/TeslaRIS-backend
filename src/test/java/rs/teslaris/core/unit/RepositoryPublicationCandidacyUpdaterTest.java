package rs.teslaris.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.applicationevent.PublicVisibilityChangedEvent;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.core.model.document.Document;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.repository.document.DocumentRepository;
import rs.teslaris.core.revision.RepositoryPublicationCandidacyUpdater;
import rs.teslaris.core.util.visibility.PublicVisibility;

public class RepositoryPublicationCandidacyUpdaterTest {

    private static final Integer DOCUMENT_ID = 7;

    private static final String ENTITY_TYPE = DocumentPublicationType.JOURNAL_PUBLICATION.name();

    private DocumentRepository documentRepository;

    private DocumentPublicationIndexRepository indexRepository;

    private ApplicationEventPublisher applicationEventPublisher;

    private RepositoryPublicationCandidacyUpdater<Document, DocumentPublicationIndex> updater;

    private static JournalPublication approvedDocument(Boolean candidate) {
        var document = new JournalPublication();
        document.setId(DOCUMENT_ID);
        document.setApproveStatus(ApproveStatus.APPROVED);
        document.setPublicationCandidate(candidate);
        return document;
    }

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        indexRepository = mock(DocumentPublicationIndexRepository.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        updater = new RepositoryPublicationCandidacyUpdater<>(
            Set.of(ENTITY_TYPE),
            documentRepository,
            PublicVisibility::isPubliclyVisible,
            indexRepository::findDocumentPublicationIndexByDatabaseId,
            indexRepository,
            applicationEventPublisher);
    }

    @Test
    void shouldExposeEntityTypes() {
        assertEquals(Set.of(ENTITY_TYPE), updater.entityTypes());
    }

    @Test
    void shouldHideRecordThatStopsBeingCandidate() {
        // Given
        var document = approvedDocument(true);
        var index = new DocumentPublicationIndex();
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(indexRepository.findDocumentPublicationIndexByDatabaseId(DOCUMENT_ID))
            .thenReturn(Optional.of(index));

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, false);

        // Then
        assertFalse(document.getPublicationCandidate());
        assertFalse(index.getIsPubliclyVisible());
        verify(documentRepository).save(document);
        verify(indexRepository).save(index);
        verify(applicationEventPublisher).publishEvent(
            new PublicVisibilityChangedEvent(ENTITY_TYPE, DOCUMENT_ID, false));
    }

    @Test
    void shouldShowRecordThatBecomesCandidate() {
        // Given
        var document = approvedDocument(false);
        var index = new DocumentPublicationIndex();
        index.setIsPubliclyVisible(false);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(indexRepository.findDocumentPublicationIndexByDatabaseId(DOCUMENT_ID))
            .thenReturn(Optional.of(index));

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, true);

        // Then
        assertTrue(document.getPublicationCandidate());
        assertTrue(index.getIsPubliclyVisible());
        verify(indexRepository).save(index);
    }

    @Test
    void shouldKeepUnapprovedCandidateHidden() {
        // Given
        var document = approvedDocument(false);
        document.setApproveStatus(ApproveStatus.REQUESTED);
        var index = new DocumentPublicationIndex();
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(indexRepository.findDocumentPublicationIndexByDatabaseId(DOCUMENT_ID))
            .thenReturn(Optional.of(index));

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, true);

        // Then
        assertTrue(document.getPublicationCandidate());
        assertFalse(index.getIsPubliclyVisible());
    }

    @Test
    void shouldSkipUnchangedCandidacy() {
        // Given
        var document = approvedDocument(true);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, true);

        // Then
        verify(documentRepository, never()).save(any());
        verify(indexRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldSkipMissingRecord() {
        // Given
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, false);

        // Then
        verify(documentRepository, never()).save(any());
        verify(indexRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldStoreCandidacyWhenRecordIsNotIndexed() {
        // Given
        var document = approvedDocument(true);
        when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
        when(indexRepository.findDocumentPublicationIndexByDatabaseId(DOCUMENT_ID))
            .thenReturn(Optional.empty());

        // When
        updater.apply(ENTITY_TYPE, DOCUMENT_ID, false);

        // Then
        verify(documentRepository).save(document);
        verify(indexRepository, never()).save(any());
    }
}
