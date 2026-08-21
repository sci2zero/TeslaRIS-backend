package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.exceptionhandling.exception.RevisionRestoreException;
import rs.teslaris.core.util.restoration.RestorationContext;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.RevisionCreateEvent;
import rs.teslaris.revisioner.model.RevisionType;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.restorer.RevisionRestorer;
import rs.teslaris.revisioner.service.impl.RevisionServiceImpl;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.RevisionConfigurationLoader;
import rs.teslaris.revisioner.util.RevisionHydratorRegistry;
import rs.teslaris.revisioner.util.RevisionRestorerRegistry;

@SpringBootTest
public class RevisionServiceTest {

    private static final String ENTITY_TYPE = DocumentPublicationType.INTANGIBLE_PRODUCT.name();

    @Mock
    private EntityRevisionRepository revisionRepository;

    @Mock
    private RevisionHydratorRegistry revisionHydratorRegistry;

    @Mock
    private RevisionRestorerRegistry revisionRestorerRegistry;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private RevisionServiceImpl revisionService;

    private MockedStatic<RevisionConfigurationLoader> mockConfigurationLoader() {
        var configurationLoader = mockStatic(RevisionConfigurationLoader.class);

        configurationLoader
            .when(() -> RevisionConfigurationLoader.listExcludedFieldsForType(any()))
            .thenReturn(Set.of());
        configurationLoader
            .when(() -> RevisionConfigurationLoader.getMigrationMappings(any()))
            .thenReturn(Map.of());

        return configurationLoader;
    }

    private EntityRevision revisionWithContent(String json, Integer major, Integer minor) {
        return revisionWithContent(json, major, minor, "hash");
    }

    private EntityRevision revisionWithContent(String json, Integer major, Integer minor,
                                               String contentHash) {
        return EntityRevision.builder()
            .majorVersion(major)
            .minorVersion(minor)
            .entityType(ENTITY_TYPE)
            .entityId(1)
            .revisionTimestamp(Instant.now())
            .contentHash(contentHash)
            .compressedContent(CompressionUtil.compress(json))
            .build();
    }

    @Test
    public void shouldCreateInitialRevisionForNewEntity() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
            RevisionType.CREATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var savedRevision = captor.getValue();
            assertEquals(1, savedRevision.getMajorVersion());
            assertEquals(0, savedRevision.getMinorVersion());
            assertEquals(ENTITY_TYPE, savedRevision.getEntityType());
            assertEquals(1, savedRevision.getEntityId());
            assertNotNull(savedRevision.getContentHash());
            assertTrue(CompressionUtil.decompress(savedRevision.getCompressedContent())
                .contains("Title"));

            verify(applicationEventPublisher).publishEvent(any(DataQualityAssessmentEvent.class));
        }
    }

    @Test
    public void shouldNotCreateRevisionWhenContentIsUnchanged() {
        // given (the entity is already under revisioning, so an edit that changes nothing is a no-op)
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, new DummyDTO(1, "Title"),
            new DummyDTO(1, "Title"), RevisionType.UPDATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 1, 0)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            verify(revisionRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    public void shouldCreateFirstRevisionForUnchangedUpdateWhenEntityHasNoRevisions() {
        // given (an entity that predates revisioning - its current state has to be recorded even
        // though the edit itself changed nothing)
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, new DummyDTO(1, "Title"),
            new DummyDTO(1, "Title"), RevisionType.UPDATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var savedRevision = captor.getValue();
            assertEquals(1, savedRevision.getMajorVersion());
            assertEquals(0, savedRevision.getMinorVersion());
            assertTrue(CompressionUtil.decompress(savedRevision.getCompressedContent())
                .contains("Title"));

            verify(applicationEventPublisher).publishEvent(any(DataQualityAssessmentEvent.class));
        }
    }

    @Test
    public void shouldIncrementMinorVersionOnUpdate() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, new DummyDTO(1, "Old title"),
            new DummyDTO(1, "New title"), RevisionType.UPDATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 2, 3)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            assertEquals(2, captor.getValue().getMajorVersion());
            assertEquals(4, captor.getValue().getMinorVersion());
        }
    }

    @Test
    public void shouldIncrementMajorVersionOnEnrichment() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Enriched"),
            RevisionType.ENRICHMENT);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 2, 3)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            assertEquals(3, captor.getValue().getMajorVersion());
            assertEquals(4, captor.getValue().getMinorVersion());
        }
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenRevisionCreationFails() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
            RevisionType.CREATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());
        when(revisionRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        try (var ignored = mockConfigurationLoader()) {
            // when
            assertThrows(IllegalStateException.class,
                () -> revisionService.createRevisionIfChanged(event));

            // then (IllegalStateException should be thrown)
        }
    }

    @SuppressWarnings("unchecked")
    private RevisionRestorer<DummyDTO> stubRestorerReturning(Object currentState) {
        RevisionRestorer<DummyDTO> restorer =
            (RevisionRestorer<DummyDTO>) mock(RevisionRestorer.class);

        doReturn(currentState).when(restorer).readCurrentState(1);
        doReturn(Optional.of(restorer)).when(revisionRestorerRegistry).get(ENTITY_TYPE);

        return restorer;
    }

    @Test
    public void shouldCaptureCurrentStateAsFirstRevisionWhenEntityHasNone() {
        // given
        stubRestorerReturning(new DummyDTO(1, "Title"));

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var created = revisionService.createRevisionFromCurrentState(ENTITY_TYPE, 1, "PTCRIS");

            // then
            assertTrue(created);

            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var savedRevision = captor.getValue();
            assertEquals(1, savedRevision.getMajorVersion());
            assertEquals(0, savedRevision.getMinorVersion());
            assertEquals(ENTITY_TYPE, savedRevision.getEntityType());
            assertEquals(1, savedRevision.getEntityId());
            assertEquals("revisionBackfill", savedRevision.getAdminNote());

            var savedContent = CompressionUtil.decompress(savedRevision.getCompressedContent());
            assertTrue(savedContent.contains("Title"));
            assertEquals(DigestUtils.sha256Hex(savedContent), savedRevision.getContentHash());

            var eventCaptor = ArgumentCaptor.forClass(DataQualityAssessmentEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

            assertEquals(savedRevision, eventCaptor.getValue().entityRevision());
            assertEquals(savedContent, eventCaptor.getValue().json());
        }
    }

    @Test
    public void shouldNotCaptureCurrentStateWhenEntityAlreadyHasRevisions() {
        // given
        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 2, 3)));

        // when
        var created = revisionService.createRevisionFromCurrentState(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertFalse(created);

        verify(revisionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void shouldNotCaptureCurrentStateWhenEntityTypeHasNoRestorer() {
        // given
        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());
        when(revisionRestorerRegistry.get(ENTITY_TYPE)).thenReturn(Optional.empty());

        // when
        var created = revisionService.createRevisionFromCurrentState(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertFalse(created);

        verify(revisionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void shouldNotCaptureCurrentStateWhenEntityCannotBeRead() {
        // given
        stubRestorerReturning(null);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        // when
        var created = revisionService.createRevisionFromCurrentState(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertFalse(created);

        verify(revisionRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    public void shouldReturnRevisionsAsDTOs() {
        // given
        var revision = revisionWithContent("{}", 1, 2);
        revision.setAdminNote("ENRICHMENT");
        revision.setUpdatedBy("admin");

        var assessment = DataQualityAssessment.builder()
            .profileName("PTCRIS")
            .profileVersion("1.3")
            .qualityScore(91.3)
            .publicationCandidate(true)
            .finishedAt(Instant.now())
            .build();
        revision.getAssessments().add(assessment);

        when(revisionRepository.findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(List.of(revision));

        // when
        var result = revisionService.getRevisions(ENTITY_TYPE, 1);

        // then
        assertEquals(1, result.size());
        assertEquals(1, result.getFirst().majorVersion());
        assertEquals(2, result.getFirst().minorVersion());
        assertEquals("ENRICHMENT", result.getFirst().versionNote());
        assertEquals("admin", result.getFirst().createdBy());
        assertEquals(1, result.getFirst().assessments().size());
        assertEquals("PTCRIS", result.getFirst().assessments().getFirst().profileName());
        assertEquals(91.3, result.getFirst().assessments().getFirst().dataQualityScore());
        assertTrue(result.getFirst().assessments().getFirst().publicationCandidate());
    }

    @Test
    public void shouldReturnEmptyListWhenEntityHasNoRevisions() {
        // given
        when(revisionRepository.findByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(List.of());

        // when
        var result = revisionService.getRevisions(ENTITY_TYPE, 1);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnRevisionContentAtTimestamp() {
        // given
        var timestamp = Instant.now();
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Title\"}", 1, 0);

        doReturn(DummyDTO.class).when(revisionHydratorRegistry).getDtoClass(ENTITY_TYPE);
        when(revisionHydratorRegistry.get(ENTITY_TYPE)).thenReturn(Optional.empty());
        when(revisionRepository
            .findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, timestamp)).thenReturn(Optional.of(revision));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = revisionService.getRevisionAtTimestamp(ENTITY_TYPE, 1, timestamp);

            // then
            assertTrue(result.isPresent());
            assertTrue(result.get().contains("Title"));
        }
    }

    @Test
    public void shouldReturnEmptyOptionalWhenNoRevisionExistsAtTimestamp() {
        // given
        var timestamp = Instant.now();

        doReturn(DummyDTO.class).when(revisionHydratorRegistry).getDtoClass(ENTITY_TYPE);
        when(revisionRepository
            .findTopByEntityTypeAndEntityIdAndRevisionTimestampLessThanEqualOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, timestamp)).thenReturn(Optional.empty());

        // when
        var result = revisionService.getRevisionAtTimestamp(ENTITY_TYPE, 1, timestamp);

        // then
        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private RevisionRestorer<DummyDTO> stubRestorerFor(EntityRevision revision) {
        RevisionRestorer<DummyDTO> restorer =
            (RevisionRestorer<DummyDTO>) mock(RevisionRestorer.class);
        when(restorer.dtoClass()).thenReturn(DummyDTO.class);

        doReturn(Optional.of(restorer)).when(revisionRestorerRegistry).get(ENTITY_TYPE);
        when(revisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, revision.getMajorVersion(), revision.getMinorVersion()))
            .thenReturn(Optional.of(revision));

        return restorer;
    }

    @Test
    public void shouldRestoreRevisionToRequestedVersion() {
        // given
        var restoredContent = "{\"id\":1,\"title\":\"Old title\"}";
        var revision = revisionWithContent(restoredContent, 1, 2);
        var restorer = stubRestorerFor(revision);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 1, 4)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 1, 2);

            // then
            verify(restorer).restore(1, new DummyDTO(1, "Old title"));

            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var savedRevision = captor.getValue();
            assertEquals(ENTITY_TYPE, savedRevision.getEntityType());
            assertEquals(1, savedRevision.getEntityId());

            // The restorer cannot read the entity back, so the requested state is recorded and the
            // hash is computed from it rather than copied from the restored revision.
            var savedContent = CompressionUtil.decompress(savedRevision.getCompressedContent());
            assertEquals(restoredContent, savedContent);
            assertEquals(DigestUtils.sha256Hex(savedContent), savedRevision.getContentHash());
            assertTrue(savedRevision.getRestorationWarnings().isEmpty());

            var eventCaptor = ArgumentCaptor.forClass(DataQualityAssessmentEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());

            assertEquals(savedRevision, eventCaptor.getValue().entityRevision());
            assertTrue(eventCaptor.getValue().json().contains("Old title"));
        }
    }

    @Test
    public void shouldRecordStateEntityActuallyReachedWhenRestorerCanReadItBack() {
        // given
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Old title\"}", 1, 2);
        var restorer = stubRestorerFor(revision);

        // Restoring dropped something, so the live entity differs from what was asked for.
        doReturn(new DummyDTO(1, "Old title (degraded)")).when(restorer).readCurrentState(1);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 1, 4)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 1, 2);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var savedRevision = captor.getValue();
            var savedContent = CompressionUtil.decompress(savedRevision.getCompressedContent());

            assertTrue(savedContent.contains("Old title (degraded)"));
            assertEquals(DigestUtils.sha256Hex(savedContent), savedRevision.getContentHash());
        }
    }

    @Test
    public void shouldDoNothingWhenRequestedRevisionIsAlreadyTheLatestOne() {
        // given
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Current title\"}", 4, 2);
        var restorer = stubRestorerFor(revision);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revision));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 4, 2);

            // then
            verify(restorer, never()).restore(any(), any());
            verify(revisionRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    public void shouldCreateMinorVersionWhenRestoringWithinSameMajorLine() {
        // given
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Old title\"}", 4, 1);
        stubRestorerFor(revision);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 4, 2)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 4, 1);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            assertEquals(4, captor.getValue().getMajorVersion());
            assertEquals(3, captor.getValue().getMinorVersion());
        }
    }

    @Test
    public void shouldCreateMajorVersionWhenRestoringFromDifferentMajorLine() {
        // given
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Old title\"}", 3, 4);
        stubRestorerFor(revision);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithContent("{}", 4, 2)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 3, 4);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            assertEquals(5, captor.getValue().getMajorVersion());
            assertEquals(0, captor.getValue().getMinorVersion());
        }
    }

    @Test
    public void shouldNotCreateRevisionWhenUpdateMatchesLatestStoredContent() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
            RevisionType.CREATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            revisionService.createRevisionIfChanged(event);

            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var storedHash = captor.getValue().getContentHash();

            when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1)).thenReturn(
                Optional.of(revisionWithContent("{}", 1, 0, storedHash)));

            // when (the same state is reported again)
            revisionService.createRevisionIfChanged(event);

            // then (no second revision is created)
            verify(revisionRepository).save(any());
        }
    }

    @Test
    public void shouldNotCreateRevisionForUpdateTriggeredByRestoration() {
        // given (the edit a restorer performs - the restore records its own revision)
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, new DummyDTO(1, "Old title"),
            new DummyDTO(1, "New title"), RevisionType.UPDATE, true);

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            verify(revisionRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Test
    public void shouldFlagEventsCreatedWhileRestorationIsInProgress() {
        // given
        var eventsDuringRestoration = RestorationContext.collectDuring(() -> {
            assertTrue(new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
                RevisionType.UPDATE).duringRestoration());
            return null;
        });

        // then (context is closed again, so ordinary edits are unaffected)
        assertTrue(eventsDuringRestoration.isEmpty());
        assertFalse(new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
            RevisionType.UPDATE).duringRestoration());
    }

    @Test
    public void shouldThrowRevisionRestoreExceptionWhenEntityTypeIsNotSupported() {
        // given
        when(revisionRestorerRegistry.get(ENTITY_TYPE)).thenReturn(Optional.empty());

        // when
        assertThrows(RevisionRestoreException.class,
            () -> revisionService.restoreRevision(ENTITY_TYPE, 1, 1, 2));

        // then (RevisionRestoreException should be thrown)
        verify(revisionRepository, never())
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowNotFoundExceptionWhenRequestedVersionDoesNotExist() {
        // given
        RevisionRestorer<DummyDTO> restorer =
            (RevisionRestorer<DummyDTO>) mock(RevisionRestorer.class);

        doReturn(Optional.of(restorer)).when(revisionRestorerRegistry).get(ENTITY_TYPE);
        when(revisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 9, 9)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> revisionService.restoreRevision(ENTITY_TYPE, 1, 9, 9));

        // then (NotFoundException should be thrown)
        verify(restorer, never()).restore(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldThrowRevisionRestoreExceptionWhenStoredContentIsNotDeserializable() {
        // given
        var revision = revisionWithContent("{\"id\":\"not-a-number\"}", 1, 2);

        RevisionRestorer<DummyDTO> restorer =
            (RevisionRestorer<DummyDTO>) mock(RevisionRestorer.class);
        when(restorer.dtoClass()).thenReturn(DummyDTO.class);

        doReturn(Optional.of(restorer)).when(revisionRestorerRegistry).get(ENTITY_TYPE);
        when(revisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 1, 2)).thenReturn(Optional.of(revision));

        try (var ignored = mockConfigurationLoader()) {
            // when
            assertThrows(RevisionRestoreException.class,
                () -> revisionService.restoreRevision(ENTITY_TYPE, 1, 1, 2));

            // then (RevisionRestoreException should be thrown)
            verify(restorer, never()).restore(any(), any());
            verify(revisionRepository, never()).save(any());
        }
    }

    @Test
    public void shouldExcludeConfiguredFieldsFromStoredContent() {
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, null, new DummyDTO(1, "Title"),
            RevisionType.CREATE);

        when(revisionRepository.findFirstByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        try (var configurationLoader = mockStatic(RevisionConfigurationLoader.class)) {
            configurationLoader
                .when(() -> RevisionConfigurationLoader.listExcludedFieldsForType(ENTITY_TYPE))
                .thenReturn(Set.of("title"));

            // when
            revisionService.createRevisionIfChanged(event);

            // then
            var captor = ArgumentCaptor.forClass(EntityRevision.class);
            verify(revisionRepository).save(captor.capture());

            var storedContent =
                CompressionUtil.decompress(captor.getValue().getCompressedContent());
            assertTrue(storedContent.contains("id"));
            assertFalse(storedContent.contains("title"));
        }
    }

    private record DummyDTO(Integer id, String title) {
    }
}
