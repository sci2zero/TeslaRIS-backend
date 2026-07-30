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
        return EntityRevision.builder()
            .majorVersion(major)
            .minorVersion(minor)
            .entityType(ENTITY_TYPE)
            .entityId(1)
            .revisionTimestamp(Instant.now())
            .contentHash("hash")
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
        // given
        var event = new RevisionCreateEvent(ENTITY_TYPE, 1, new DummyDTO(1, "Title"),
            new DummyDTO(1, "Title"), RevisionType.UPDATE);

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.createRevisionIfChanged(event);

            // then
            verify(revisionRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
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

    @Test
    @SuppressWarnings("unchecked")
    public void shouldRestoreRevisionToRequestedVersion() {
        // given
        var revision = revisionWithContent("{\"id\":1,\"title\":\"Old title\"}", 1, 2);

        RevisionRestorer<DummyDTO> restorer =
            (RevisionRestorer<DummyDTO>) mock(RevisionRestorer.class);
        when(restorer.dtoClass()).thenReturn(DummyDTO.class);

        doReturn(Optional.of(restorer)).when(revisionRestorerRegistry).get(ENTITY_TYPE);
        when(revisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 1, 2)).thenReturn(Optional.of(revision));

        try (var ignored = mockConfigurationLoader()) {
            // when
            revisionService.restoreRevision(ENTITY_TYPE, 1, 1, 2);

            // then
            verify(restorer).restore(1, new DummyDTO(1, "Old title"));
        }
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
