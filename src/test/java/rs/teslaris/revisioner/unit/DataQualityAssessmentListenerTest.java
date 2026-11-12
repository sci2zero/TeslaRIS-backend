package rs.teslaris.revisioner.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.ApplicationEventPublisher;
import rs.teslaris.core.applicationevent.PublicationCandidacyChangedEvent;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.util.DataQualityCalculator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentListener;

public class DataQualityAssessmentListenerTest {

    private static final String PROFILE = "ptcris";

    private static final String VERSION = "1.0.0";

    private static final Integer ENTITY_ID = 11;

    private DataQualityCalculator calculator;

    private EntityRevisionRepository entityRevisionRepository;

    private ApplicationEventPublisher applicationEventPublisher;

    private DataQualityAssessmentListener listener;

    private MockedStatic<DataQualityAssessmentConfigurationLoader> loader;

    private static EntityRevision revision(Integer id) {
        var revision = EntityRevision.builder()
            .entityType(EntityType.PERSON.name())
            .entityId(ENTITY_ID)
            .build();
        revision.setId(id);
        return revision;
    }

    @BeforeEach
    void setUp() {
        calculator = mock(DataQualityCalculator.class);
        entityRevisionRepository = mock(EntityRevisionRepository.class);
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        listener = new DataQualityAssessmentListener(calculator, entityRevisionRepository,
            mock(DataQualityAssessmentRepository.class), applicationEventPublisher);

        loader = mockStatic(DataQualityAssessmentConfigurationLoader.class);
        loader.when(DataQualityAssessmentConfigurationLoader::listAvailableProfiles)
            .thenReturn(Set.of(PROFILE));
        loader.when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(PROFILE))
            .thenReturn(VERSION);
        loader.when(() -> DataQualityAssessmentConfigurationLoader.getTargetTypesFromEntityType(
            EntityType.PERSON)).thenReturn(List.of("Person"));
    }

    @AfterEach
    void tearDown() {
        loader.close();
    }

    private void assessAs(boolean publicationCandidate) {
        when(calculator.assessDataQuality(any(), anyString(), any(), any(), anyList()))
            .thenAnswer(invocation -> {
                invocation.<DataQualityAssessment>getArgument(0)
                    .setPublicationCandidate(publicationCandidate);
                return true;
            });
    }

    private void profileDerivesVisibility(boolean derives) {
        loader.when(() -> DataQualityAssessmentConfigurationLoader.derivesPublicVisibility(
            PROFILE, VERSION)).thenReturn(derives);
    }

    @Test
    void shouldPublishCandidacyOfLatestRevision() {
        // Given
        var revision = revision(100);
        profileDerivesVisibility(true);
        assessAs(false);
        when(entityRevisionRepository.findLatestRevisionId(EntityType.PERSON.name(), ENTITY_ID))
            .thenReturn(Optional.of(revision.getId()));

        // When
        listener.handle(new DataQualityAssessmentEvent(revision, "{}"));

        // Then
        var captor = ArgumentCaptor.forClass(PublicationCandidacyChangedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(EntityType.PERSON.name(), captor.getValue().entityType());
        assertEquals(ENTITY_ID, captor.getValue().entityId());
        assertFalse(captor.getValue().publicationCandidate());
    }

    @Test
    void shouldNotPublishCandidacyOfOutdatedRevision() {
        // Given
        var revision = revision(100);
        profileDerivesVisibility(true);
        assessAs(false);
        when(entityRevisionRepository.findLatestRevisionId(EntityType.PERSON.name(), ENTITY_ID))
            .thenReturn(Optional.of(101));

        // When
        listener.handle(new DataQualityAssessmentEvent(revision, "{}"));

        // Then
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void shouldNotPublishCandidacyOfProfileThatDoesNotDeriveVisibility() {
        // Given
        var revision = revision(100);
        profileDerivesVisibility(false);
        assessAs(false);

        // When
        listener.handle(new DataQualityAssessmentEvent(revision, "{}"));

        // Then
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
        verify(entityRevisionRepository, never()).findLatestRevisionId(anyString(), any());
    }

    @Test
    void shouldNotPublishWhenNothingWasAssessed() {
        // Given
        var revision = revision(100);
        profileDerivesVisibility(true);
        when(calculator.assessDataQuality(any(), anyString(), any(), any(), anyList()))
            .thenReturn(false);

        // When
        listener.handle(new DataQualityAssessmentEvent(revision, "{}"));

        // Then
        verify(applicationEventPublisher, never()).publishEvent(any(Object.class));
    }
}
