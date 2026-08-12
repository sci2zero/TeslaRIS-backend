package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.impl.DataQualityServiceImpl;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;

@SpringBootTest
public class DataQualityServiceTest {

    private static final String ENTITY_TYPE = DocumentPublicationType.INTANGIBLE_PRODUCT.name();

    @Mock
    private EntityRevisionRepository entityRevisionRepository;

    @Mock
    private LanguageTagService languageTagService;

    @InjectMocks
    private DataQualityServiceImpl dataQualityService;


    private MultiLingualContent multilingualContent(String content) {
        var languageTag = new LanguageTag();
        languageTag.setId(1);
        languageTag.setLanguageTag("EN");

        return new MultiLingualContent(languageTag, content, 1);
    }

    private DataQualityAssessment assessmentWithIssue(ConstraintEvaluationResult issue) {
        var revision = EntityRevision.builder()
            .entityType(ENTITY_TYPE)
            .entityId(1)
            .revisionTimestamp(Instant.now())
            .build();

        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName("PTCRIS")
            .profileVersion("1.3")
            .qualityScore(91.3)
            .publicationCandidate(true)
            .infoFailedRules(1)
            .warningFailedRules(2)
            .errorFailedRules(3)
            .startedAt(Instant.now())
            .finishedAt(Instant.now())
            .issues(Objects.isNull(issue) ? List.of() : List.of(issue))
            .build();

        revision.getAssessments().add(assessment);

        return assessment;
    }

    @Test
    public void shouldReturnEmptyReportWhenEntityHasNoRevisions() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        // when
        var result = dataQualityService.getQualityReportForEntity(ENTITY_TYPE, 1);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldReturnQualityReportForEntity() {
        // given
        var issue = ConstraintEvaluationResult.builder()
            .key("missingAbstract")
            .parameters(List.of("abstract"))
            .severity(IssueSeverity.ERROR)
            .dimension(QualityDimension.COMPLETENESS)
            .build();

        var assessment = assessmentWithIssue(issue);

        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(assessment.getRevision()));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()))
                .thenReturn(Set.of(multilingualContent("Abstract is missing.")));

            // when
            var result = dataQualityService.getQualityReportForEntity(ENTITY_TYPE, 1);

            // then
            assertEquals(1, result.size());

            var report = result.getFirst();
            assertEquals("PTCRIS (1.3)", report.profileName());
            assertEquals(91.3, report.qualityScore());
            assertEquals(6, report.issueCount());
            assertTrue(report.publicationCandidate());
            assertEquals(1, report.report().size());
            assertEquals(IssueSeverity.ERROR, report.report().getFirst().a);
            assertEquals("Abstract is missing.",
                report.report().getFirst().b.getFirst().getContent());
        }
    }

    @Test
    public void shouldReturnReportWithoutIssuesWhenAssessmentHasNone() {
        // given
        var assessment = assessmentWithIssue(null);

        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(assessment.getRevision()));

        // when
        var result = dataQualityService.getQualityReportForEntity(ENTITY_TYPE, 1);

        // then
        assertEquals(1, result.size());
        assertTrue(result.getFirst().report().isEmpty());
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenEntityHasNoAssessments() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> dataQualityService.findLatestAssessmentsForEntity(ENTITY_TYPE, 1));

        // then (NotFoundException should be thrown)
        verify(entityRevisionRepository)
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(ENTITY_TYPE, 1);
    }

    private DataQualityAssessment fullyScoredAssessment(Integer majorVersion,
                                                        Integer minorVersion) {
        var revision = EntityRevision.builder()
            .entityType(ENTITY_TYPE)
            .entityId(1)
            .majorVersion(majorVersion)
            .minorVersion(minorVersion)
            .revisionTimestamp(Instant.now())
            .build();

        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName("PTCRIS")
            .profileVersion("1.3")
            .engineVersion("1.0.0")
            .valid(true)
            .publicationCandidate(true)
            .passedRules(42)
            .infoFailedRules(0)
            .warningFailedRules(1)
            .errorFailedRules(1)
            .totalPoints(100.0)
            .achievedPointsNormalised(91.3)
            .qualityScore(91.3)
            .totalPointsFair(50.0)
            .achievedFairPointsNormalised(43.5)
            .qualityScoreFair(87.0)
            .startedAt(Instant.now())
            .finishedAt(Instant.now())
            .issues(List.of())
            .build();

        revision.getAssessments().add(assessment);

        return assessment;
    }

    @Test
    public void shouldReturnAssessmentsForRequestedVersion() {
        // given
        var assessment = fullyScoredAssessment(4, 2);

        when(entityRevisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 4, 2)).thenReturn(Optional.of(assessment.getRevision()));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getTargetTypesFromDocumentType(
                    any()))
                .thenReturn(List.of("Document"));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getRulesForTarget(
                    anyString(), anyString(), any()))
                .thenReturn(List.of());

            // when
            var result = dataQualityService.findAssessmentsForEntityVersion(ENTITY_TYPE, 1, 4, 2);

            // then
            assertEquals(1, result.size());

            var assessmentDTO = result.getFirst();
            assertEquals("PTCRIS", assessmentDTO.profileName());
            assertEquals("1.3", assessmentDTO.profileVersion());
            assertEquals(91.3, assessmentDTO.qualityScore());
            assertEquals(87.0, assessmentDTO.qualityScoreFair());
            assertEquals(50.0, assessmentDTO.totalPointsFair());
            assertEquals(43.5, assessmentDTO.achievedFairPoints());
            assertEquals(42, assessmentDTO.passedRules());
            assertTrue(assessmentDTO.valid());
        }
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenRequestedVersionDoesNotExist() {
        // given
        when(entityRevisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 9, 9)).thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> dataQualityService.findAssessmentsForEntityVersion(ENTITY_TYPE, 1, 9, 9));

        // then (NotFoundException should be thrown)
        verify(entityRevisionRepository)
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                ENTITY_TYPE, 1, 9, 9);
    }

    @Test
    public void shouldReturnEmptyProfileListWhenNoProfilesAreConfigured() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
                .thenReturn(new LinkedHashSet<>());

            // when
            var result = dataQualityService.listAllDataQualityProfiles();

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void shouldReturnAllConfiguredDataQualityProfiles() {
        // given
        var profile = new DataQualityAssessmentConfigurationLoader.DataQualityProfile(
            "1.3", 70.0, Map.of(), Map.of(), Map.of(), Map.of(),
            Map.of("DOCUMENT", new EnumMap<>(QualityDimension.class)));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
                .thenReturn(new LinkedHashSet<>(List.of(new Pair<>("PTCRIS", "1.3"))));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getProfile("PTCRIS", "1.3"))
                .thenReturn(profile);

            // when
            var result = dataQualityService.listAllDataQualityProfiles();

            // then
            assertEquals(1, result.size());
            assertEquals("1.3", result.getFirst().version());
            assertTrue(result.getFirst().dataQualityRemarks().isEmpty());
        }
    }
}
