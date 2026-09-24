package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.dto.RelatedQualityDTO;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.EntityRevision;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.impl.DataQualityServiceImpl;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.dataquality.DataQualityAggregator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.IssueCursor;
import rs.teslaris.revisioner.util.dataquality.RelatedEntityType;

@SpringBootTest
public class DataQualityServiceTest {

    private static final String ENTITY_TYPE = DocumentPublicationType.INTANGIBLE_PRODUCT.name();

    private static final String PERSON_ENTITY_TYPE = EntityType.PERSON.name();

    private static final String ORGANISATION_UNIT_ENTITY_TYPE =
        EntityType.ORGANISATION_UNIT.name();

    @Mock
    private EntityRevisionRepository entityRevisionRepository;

    @Mock
    private LanguageTagService languageTagService;

    @Mock
    private SearchService<DataQualityAssessmentIndex> searchService;

    @Mock
    private DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    @Mock
    private DataQualityAssessmentRepository dataQualityAssessmentRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private DataQualityAggregator dataQualityAggregator;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private PersonIndexRepository personIndexRepository;

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
            .dimension(QualityDimension.CONSISTENCY)
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

    private String capturedScanQuery() {
        var captor = ArgumentCaptor.forClass(Query.class);
        verify(searchService, atLeastOnce()).runQueryWithoutTotal(captor.capture(), any(),
            eq(DataQualityAssessmentIndex.class), anyString());

        return captor.getValue().toString();
    }

    private RelatedQualityDTO row(List<RelatedQualityDTO> rows, RelatedEntityType entityType) {
        return rows.stream()
            .filter(row -> row.entityType() == entityType)
            .findFirst()
            .orElseThrow();
    }

    private void stubIssueTotal(long total) {
        when(dataQualityAggregator.countIssues(any(), any())).thenReturn(OptionalLong.of(total));
    }

    private void stubIssueConfiguration(
        MockedStatic<DataQualityAssessmentConfigurationLoader> configurationLoader) {
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                anyString(), anyString(), any(), any(), any()))
            .thenReturn(new LinkedHashSet<>(List.of("titleMissing")));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                anyString(), anyString(), anyString()))
            .thenReturn(remark(IssueSeverity.ERROR, QualityDimension.CONSISTENCY));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                anyString(), anyString(), anyString()))
            .thenReturn(Set.of(multilingualContent("Title")));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                anyString(), anyString(), anyString(), any()))
            .thenReturn(Set.of(multilingualContent("Message")));
    }

    private DataQualityAssessmentIndex assessmentIndex(Integer entityId, String target,
                                                       List<String> failedRuleKeys) {
        var index = new DataQualityAssessmentIndex();
        index.setId(String.valueOf(entityId));
        index.setEntityType(ENTITY_TYPE);
        index.setEntityId(entityId);
        index.setTarget(target);
        index.setProfileName("PTCRIS");
        index.setProfileVersion("1.3");
        index.setAssessmentDate(LocalDateTime.now());
        index.setRecordMajorVersion(4);
        index.setRecordMinorVersion(2);
        index.setFailedRuleKeys(failedRuleKeys);

        return index;
    }

    private DataQualityAssessmentConfigurationLoader.DataQualityRemark remark(
        IssueSeverity severity, QualityDimension dimension) {
        return new DataQualityAssessmentConfigurationLoader.DataQualityRemark(
            Map.of("en", "Title"), Map.of("en", "Message"), "Document", severity, dimension,
            true, 5.0, true, Map.of());
    }

    @Test
    public void shouldReturnPageOfIssuesForEntity() {
        // given
        var index = assessmentIndex(1, "Document", List.of("titleMissing", "doiNotResolvable"));

        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(index)));

        stubIssueTotal(2);

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of("titleMissing", "doiNotResolvable")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                    anyString(), anyString(), anyString()))
                .thenReturn(remark(IssueSeverity.ERROR, QualityDimension.CONSISTENCY));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title is missing.")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()))
                .thenReturn(Set.of(multilingualContent("Title is missing.")));

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, null, 10);

            // then
            assertEquals(2, result.totalIssues());
            assertEquals(2, result.content().size());
            assertEquals("doiNotResolvable", result.content().getFirst().ruleKey());
            assertEquals(4, result.content().getFirst().recordMajorVersion());
            assertEquals(IssueSeverity.ERROR, result.content().getFirst().severity());
        }
    }

    @Test
    public void shouldReturnOnlyIssuesMatchingConstraintFilter() {
        // given
        var index = assessmentIndex(1, "Document", List.of("titleMissing", "doiNotResolvable"));

        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(index)));

        stubIssueTotal(1);

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of("titleMissing", "doiNotResolvable")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                    anyString(), anyString(), anyString()))
                .thenReturn(remark(IssueSeverity.WARNING, QualityDimension.ACCURACY));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()))
                .thenReturn(Set.of(multilingualContent("Message")));

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, "titleMissing", null, null, 10);

            // then
            assertEquals(1, result.totalIssues());
            assertEquals("titleMissing", result.content().getFirst().ruleKey());
        }
    }

    @Test
    public void shouldSkipRuleKeysFilteredOutByDimensionOrSeverity() {
        // given
        var index = assessmentIndex(1, "Document", List.of("titleMissing", "doiNotResolvable"));

        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(index)));

        stubIssueTotal(1);

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of("doiNotResolvable")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                    anyString(), anyString(), anyString()))
                .thenReturn(remark(IssueSeverity.ERROR, QualityDimension.ACCURACY));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()))
                .thenReturn(Set.of(multilingualContent("Message")));

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                QualityDimension.ACCURACY, IssueSeverity.ERROR, null, null, null, 10);

            // then
            assertEquals(1, result.totalIssues());
            assertEquals("doiNotResolvable", result.content().getFirst().ruleKey());
        }
    }

    @Test
    public void shouldResumeAfterTheCursorRow() {
        // given (two records, each contributing one issue)
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")),
            assessmentIndex(2, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            var firstPage = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS",
                null, null, null, null, null, null, 1);

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, firstPage.nextCursor(), 1);

            // then (the window starts after the row the cursor names, not after a document count)
            assertEquals(1, firstPage.content().size());
            assertEquals(1, firstPage.content().getFirst().entityId());
            assertEquals(1, result.content().size());
            assertEquals(2, result.content().getFirst().entityId());
            assertNull(result.nextCursor());
        }
    }

    @Test
    public void shouldResumeInsideARecordWithSeveralIssues() {
        // given (one record with three issues, served one per page)
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document",
                List.of("titleMissing", "doiNotResolvable", "abstractMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(
                    List.of("titleMissing", "doiNotResolvable", "abstractMissing")));

            var seen = new ArrayList<String>();
            String cursor = null;

            // when (walk the record one row at a time)
            do {
                var page = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                    null, null, null, null, cursor, 1);

                page.content().forEach(issue -> seen.add(issue.ruleKey()));
                cursor = page.nextCursor();
            } while (cursor != null);

            // then (every issue exactly once, in rule-key order)
            assertEquals(List.of("abstractMissing", "doiNotResolvable", "titleMissing"), seen);
        }
    }

    @Test
    public void shouldNotOfferACursorWhenThePageIsExactlyTheLastRows() {
        // given (two issues, page size two)
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")),
            assessmentIndex(2, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, null, 2);

            // then
            assertEquals(2, result.content().size());
            assertNull(result.nextCursor());
        }
    }

    @Test
    public void shouldClampThePageSize() {
        // given
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when (a size no caller may ask for, and no size at all)
            var oversized = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, null, 10000);
            var defaulted = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, null, null);

            // then (both serve the one row there is, neither fails)
            assertEquals(1, oversized.content().size());
            assertEquals(1, defaulted.content().size());
        }
    }

    @Test
    public void shouldListTheIssuesCurrentOnTheRequestedDay() {
        // given
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of()));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null, null, null,
                null, LocalDate.of(2026, 7, 18), null, 10);

            // then (the two validity ranges at the end of that day, and no is_latest clause)
            var query = capturedScanQuery();
            assertTrue(query.contains("assessment_date"));
            assertTrue(query.contains("valid_to"));
            assertTrue(query.contains("2026-07-18T23:59:59.999"));
            assertFalse(query.contains("is_latest"));
        }
    }

    @Test
    public void shouldListTheLatestIssuesWhenNoDayIsRequested() {
        // given
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of()));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null, null, null,
                null, null, null, 10);

            // then
            var query = capturedScanQuery();
            assertTrue(query.contains("is_latest"));
            assertFalse(query.contains("valid_to"));
        }
    }

    @Test
    public void shouldListRepositoryIssuesWithoutAnEntityScope() {
        // given (an admin: no unit, so no scope clause at all)
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            var result = dataQualityService.findRepositoryIssues(null, "PTCRIS", null, null,
                null, null, null, null, 10);

            // then
            assertEquals(1, result.content().size());

            var query = capturedScanQuery();
            assertFalse(query.contains("organisation_unit_ids"));
            assertFalse(query.contains("related_person_ids"));
            assertFalse(query.contains("entity_id"));
            assertTrue(query.contains("is_latest"));
        }
    }

    @Test
    public void shouldScopeRepositoryIssuesToTheUnitSubHierarchy() {
        // given (an institutional editor sees their unit and everything below it)
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8, 9));
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of()));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            dataQualityService.findRepositoryIssues(7, "PTCRIS", null, null, null, null,
                LocalDate.of(2026, 7, 18), null, 10);

            // then
            var query = capturedScanQuery();
            assertTrue(query.contains("organisation_unit_ids"));
            assertTrue(query.contains("2026-07-18T23:59:59.999"));
            assertFalse(query.contains("entity_id"));

            verify(organisationUnitService).getOrganisationUnitIdsFromSubHierarchy(7);
        }
    }

    @Test
    public void shouldRejectAMalformedCursor() {
        // when
        // then
        assertThrows(IllegalArgumentException.class,
            () -> dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null, null,
                null, null, null, "not-a-cursor", 10));
    }

    /**
     * Rendering an issue row resolves a language tag per language through the repository, twice
     * over. A deep page discards far more rows than it returns, so the rows before the offset are
     * counted rather than rendered - otherwise the scan issues hundreds of thousands of queries
     * for rows nobody sees.
     */
    @Test
    public void shouldRenderOnlyTheIssuesThatLandInTheWindow() {
        // given (three records, each contributing one issue)
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")),
            assessmentIndex(2, "Document", List.of("titleMissing")),
            assessmentIndex(3, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when (one row of three)
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, null, 1);

            // then (the lookahead row is scanned but never rendered)
            assertEquals(1, result.content().size());
            assertNotNull(result.nextCursor());

            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()),
                times(1));
            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()),
                times(1));
        }
    }

    /**
     * The applicable rule keys depend on the profile version and the filters, not on the record,
     * so they are resolved once for the whole scan.
     */
    @Test
    public void shouldResolveApplicableRuleKeysOncePerProfileVersion() {
        // given
        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(
            assessmentIndex(1, "Document", List.of("titleMissing")),
            assessmentIndex(2, "Document", List.of("titleMissing")),
            assessmentIndex(3, "Document", List.of("titleMissing")))));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader);

            // when
            dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null, null, null,
                null, null, null, 10);

            // then
            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()),
                times(1));
        }
    }

    @Test
    public void shouldReturnEmptyPageWhenTheCursorNamesTheLastRow() {
        // given
        var index = assessmentIndex(1, "Document", List.of("titleMissing"));

        when(searchService.runQueryWithoutTotal(any(), any(), eq(DataQualityAssessmentIndex.class),
            anyString())).thenReturn(new PageImpl<>(List.of(index)));

        stubIssueTotal(1);

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of("titleMissing")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                    anyString(), anyString(), anyString()))
                .thenReturn(remark(IssueSeverity.INFO, QualityDimension.CONSISTENCY));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    anyString(), anyString(), anyString(), any()))
                .thenReturn(Set.of(multilingualContent("Message")));

            var lastRow = new IssueCursor(1, IssueSeverity.INFO, "titleMissing", ENTITY_TYPE, 1);

            // when
            var result = dataQualityService.findIssuesForEntity(ENTITY_TYPE, 1, "PTCRIS", null,
                null, null, null, null, lastRow.encode(), 10);

            // then
            assertEquals(1, result.totalIssues());
            assertTrue(result.content().isEmpty());
            assertNull(result.nextCursor());
        }
    }

    private EntityRevision revisionWithProfiles(String entityType, String... profileNames) {
        var revision = EntityRevision.builder()
            .entityType(entityType)
            .entityId(1)
            .majorVersion(4)
            .minorVersion(2)
            .revisionTimestamp(Instant.now())
            .build();

        for (var profileName : profileNames) {
            revision.getAssessments().add(DataQualityAssessment.builder()
                .revision(revision)
                .profileName(profileName)
                .profileVersion("1.3")
                .startedAt(Instant.now())
                .finishedAt(Instant.now())
                .build());
        }

        return revision;
    }

    /**
     * Related quality makes up to four assessment passes per profile: outputs, persons, events
     * and publication series, then organisation units. Only the first is stubbed with figures and
     * the rest are empty, so the assertions stay about the outputs pass unless a test says
     * otherwise.
     */
    private void stubAggregates(long affectedRecords, long openIssues, long activitiesCount,
                                long activityIssues, Double averageScore, long linkedRecords,
                                long linkedActivities) {
        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(Optional.of(new DataQualityAggregator.AssessmentAggregates(
                    affectedRecords, openIssues, activitiesCount, activityIssues, activityIssues,
                    0, 0.0, 0, averageScore)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(new DataQualityAggregator.LinkedDocumentAggregates(
                linkedRecords, linkedActivities)));
    }

    @Test
    public void shouldReturnEmptyRelatedQualityWhenEntityHasNoRevisions() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        // when
        var result = dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1);

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void shouldComputeOutputsRelatedQualityForPerson() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(2, 5, 4, 1, 92.0, 186, 5);

        // when
        var result = dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1);

        // then
        assertEquals(1, result.size());

        var profile = result.getFirst();
        assertEquals("PTCRIS", profile.profileName());
        assertEquals("1.3", profile.profileVersion());
        assertEquals(6, profile.relatedQuality().size());

        var outputs = row(profile.relatedQuality(), RelatedEntityType.OUTPUTS);
        assertEquals(RelatedEntityType.OUTPUTS, outputs.entityType());
        assertTrue(outputs.supported());
        assertEquals(186, outputs.linkedRecords());
        assertEquals(2, outputs.affectedRecords());
        assertEquals(4, outputs.openIssues());
        assertEquals(92.0, outputs.averageScore());
    }

    @Test
    public void shouldComputeActivitiesRelatedQualityForPerson() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(2, 5, 4, 1, 92.0, 186, 5);

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ACTIVITIES);

        // then
        assertEquals(RelatedEntityType.ACTIVITIES, activities.entityType());
        assertTrue(activities.supported());
        assertEquals(5, activities.linkedRecords());
        assertEquals(4, activities.affectedRecords());
        assertEquals(1, activities.openIssues());

        // The fixture accumulates no activity score, and four activities were assessed.
        assertEquals(0.0, activities.averageScore());
    }

    /**
     * The score is a sum over the activities assessed, not an average over the records carrying
     * them, so a record holding one activity cannot weigh as much as one holding many.
     */
    @Test
    public void shouldAverageActivityScoreOverActivitiesRatherThanRecords() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(
                Optional.of(new DataQualityAggregator.AssessmentAggregates(
                    2, 5, 4, 1, 1, 0, 320.0, 0, 92.0)),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(
                    1, 3, 6, 2, 2, 0, 480.0, 0, 88.0)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.LinkedDocumentAggregates(186, 5)));

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ACTIVITIES);

        // then (800 points over 10 assessed activities)
        assertEquals(10, activities.affectedRecords());
        assertEquals(80.0, activities.averageScore());
    }

    @Test
    public void shouldReportNoActivityScoreWhenNoActivityWasAssessed() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(2, 5, 0, 0, 92.0, 186, 0);

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ACTIVITIES);

        // then
        assertNull(activities.averageScore());
    }

    /**
     * Involvements are activities recorded on the person, so they are counted from the person index
     * and the person assessments, on top of whatever the outputs contribute.
     */
    @Test
    public void shouldAddInvolvementActivitiesToTheActivitiesRow() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(
                Optional.of(new DataQualityAggregator.AssessmentAggregates(2, 5, 4, 1, 1, 0, 0.0, 0,
                    92.0)),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(1, 3, 6, 2, 2, 0, 0.0, 0,
                    88.0)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.LinkedDocumentAggregates(186, 5)));
        when(dataQualityAggregator.sumField(eq("person"), any(), eq("activities_count")))
            .thenReturn(3L);

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ACTIVITIES);

        // then
        assertEquals(8, activities.linkedRecords());   // 5 on outputs + 3 on the person
        assertEquals(10, activities.affectedRecords()); // 4 assessed + 6 assessed
        assertEquals(3, activities.openIssues());       // 1 + 2
    }

    /**
     * Event and publication series contributions are activities as well. A person is linked to
     * both through the assessments, but only events can be counted as records for a person, since
     * the series indexes hold no person link.
     */
    @Test
    public void shouldAddEventAndSeriesActivitiesToAPersonsActivitiesRow() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(
                Optional.of(new DataQualityAggregator.AssessmentAggregates(2, 5, 4, 1, 1, 0, 320.0,
                    0, 92.0)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(1, 2, 2, 1, 1, 0, 160.0,
                    0, 80.0)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.LinkedDocumentAggregates(186, 5)));
        when(dataQualityAggregator.sumField(eq("events"), any(), eq("activities_count")))
            .thenReturn(2L);

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ACTIVITIES);

        // then
        assertEquals(7, activities.linkedRecords());   // 5 on outputs + 2 on events
        assertEquals(6, activities.affectedRecords()); // 4 assessed + 2 assessed
        assertEquals(2, activities.openIssues());      // 1 + 1
        assertEquals(80.0, activities.averageScore()); // 480 points over 6 activities

        verify(dataQualityAggregator, never())
            .sumField(eq("journal"), any(), eq("activities_count"));
        verify(dataQualityAggregator, never())
            .sumField(eq("book_series"), any(), eq("activities_count"));
    }

    @Test
    public void shouldCountSeriesActivitiesForAnOrganisationUnitByRelatedInstitutions() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ORGANISATION_UNIT_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(ORGANISATION_UNIT_ENTITY_TYPE, "PTCRIS")));
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1));

        stubAggregates(0, 0, 0, 0, null, 0, 0);

        when(dataQualityAggregator.sumField(eq("events"), any(), eq("activities_count")))
            .thenReturn(4L);
        when(dataQualityAggregator.sumField(eq("journal"), any(), eq("activities_count")))
            .thenReturn(3L);
        when(dataQualityAggregator.sumField(eq("book_series"), any(), eq("activities_count")))
            .thenReturn(1L);

        // when
        var activities = row(dataQualityService.getRelatedQualityForEntity(
            ORGANISATION_UNIT_ENTITY_TYPE, 1).getFirst().relatedQuality(),
            RelatedEntityType.ACTIVITIES);

        // then
        assertEquals(8, activities.linkedRecords());

        var captor = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator).sumField(eq("journal"), captor.capture(),
            eq("activities_count"));
        assertTrue(captor.getValue().toString().contains("related_institution_ids"));
    }

    @Test
    public void shouldQueryRelatedAssessmentsByOrganisationUnitField() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ORGANISATION_UNIT_ENTITY_TYPE, 1))
            .thenReturn(
                Optional.of(revisionWithProfiles(ORGANISATION_UNIT_ENTITY_TYPE, "PTCRIS")));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));

        stubAggregates(1, 3, 7, 0, 80.0, 42, 7);

        // when
        var outputs =
            row(dataQualityService.getRelatedQualityForEntity(ORGANISATION_UNIT_ENTITY_TYPE, 1)
                .getFirst().relatedQuality(), RelatedEntityType.OUTPUTS);

        // then
        assertEquals(42, outputs.linkedRecords());
        assertEquals(1, outputs.affectedRecords());
        assertEquals(3, outputs.openIssues());
        assertEquals(80.0, outputs.averageScore());

        var queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator, times(4))
            .aggregateAssessments(queryCaptor.capture(), any());

        assertTrue(queryCaptor.getAllValues().getFirst().toString()
            .contains("organisation_unit_ids"));
    }

    @Test
    public void shouldScopeOrganisationUnitReportToItsSubHierarchy() {
        // given (a unit answers for the records of everything below it)
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ORGANISATION_UNIT_ENTITY_TYPE, 1))
            .thenReturn(
                Optional.of(revisionWithProfiles(ORGANISATION_UNIT_ENTITY_TYPE, "PTCRIS")));

        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2, 3));

        stubAggregates(1, 3, 7, 0, 80.0, 42, 7);

        // when
        dataQualityService.getRelatedQualityForEntity(ORGANISATION_UNIT_ENTITY_TYPE, 1);

        // then
        var assessmentQuery = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator, times(4))
            .aggregateAssessments(assessmentQuery.capture(), any());

        var documentQuery = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator).aggregateLinkedDocuments(documentQuery.capture());

        List.of(assessmentQuery.getAllValues().getFirst().toString(),
                documentQuery.getValue().toString())
            .forEach(query -> {
                assertTrue(query.contains("organisation_unit_ids"));
                assertTrue(query.contains("2"));
                assertTrue(query.contains("3"));
            });
    }

    @Test
    public void shouldNotResolveHierarchyForPersonReports() {
        // given (persons have no sub-hierarchy - their records are matched by person id)
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(2, 5, 4, 1, 92.0, 186, 5);

        // when
        dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1);

        // then
        verifyNoInteractions(organisationUnitService);
    }

    @Test
    public void shouldReturnOneEntryPerProfileSortedByProfileName() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "ZENODO", "PTCRIS")));

        stubAggregates(0, 0, 0, 0, null, 10, 0);

        // when
        var result = dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1);

        // then
        assertEquals(2, result.size());
        assertEquals("PTCRIS", result.getFirst().profileName());
        assertEquals("ZENODO", result.get(1).profileName());
    }

    @Test
    public void shouldReturnNullAverageScoreWhenNoRelatedRecordsAreAssessed() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(0, 0, 0, 0, null, 5, 0);

        // when
        var outputs = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.OUTPUTS);

        // then
        assertEquals(5, outputs.linkedRecords());
        assertEquals(0, outputs.affectedRecords());
        assertEquals(0, outputs.openIssues());
        assertNull(outputs.averageScore());
    }

    @Test
    public void shouldFallBackToZeroesWhenAggregationIsUnavailable() {
        // given (the aggregator degrades to an empty result rather than failing the tab)
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        when(dataQualityAggregator.aggregateAssessments(any(), any())).thenReturn(Optional.empty());
        when(dataQualityAggregator.aggregateLinkedDocuments(any())).thenReturn(Optional.empty());

        // when
        var outputs = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.OUTPUTS);

        // then
        assertTrue(outputs.supported());
        assertEquals(0, outputs.linkedRecords());
        assertEquals(0, outputs.affectedRecords());
        assertEquals(0, outputs.openIssues());
        assertNull(outputs.averageScore());
    }

    @Test
    public void shouldMarkAllRelatedTypesUnsupportedForEntityWithoutLinkedOutputs() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revisionWithProfiles(ENTITY_TYPE, "PTCRIS")));

        // when
        var relatedQuality = dataQualityService.getRelatedQualityForEntity(ENTITY_TYPE, 1)
            .getFirst().relatedQuality();

        // then
        assertEquals(6, relatedQuality.size());
        assertTrue(relatedQuality.stream().noneMatch(RelatedQualityDTO::supported));

        verifyNoInteractions(dataQualityAggregator);
    }

    /**
     * A unit's related persons are everyone employed below it; their involvement issues belong to
     * the Activities row, so they are left out of the Persons count.
     */
    @Test
    public void shouldComputeRelatedPersonsForAnOrganisationUnit() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ORGANISATION_UNIT_ENTITY_TYPE, 1))
            .thenReturn(
                Optional.of(revisionWithProfiles(ORGANISATION_UNIT_ENTITY_TYPE, "PTCRIS")));
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1, 2));
        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(
                Optional.of(new DataQualityAggregator.AssessmentAggregates(2, 5, 4, 1, 1, 0, 0.0, 0,
                    92.0)),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(30, 50, 6, 8, 8, 0, 0.0,
                    0, 88.0)),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(2, 3, 0, 0, 0, 0, 0.0, 0,
                    75.0)));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(new DataQualityAggregator.LinkedDocumentAggregates(10, 0)));
        when(dataQualityAggregator.countRecords(eq("person"), any())).thenReturn(48L);
        when(dataQualityAggregator.countRecords(eq("organisation_unit"), any())).thenReturn(2L);

        // when
        var rows = dataQualityService.getRelatedQualityForEntity(ORGANISATION_UNIT_ENTITY_TYPE, 1)
            .getFirst().relatedQuality();

        // then
        var persons = row(rows, RelatedEntityType.PERSONS);
        assertTrue(persons.supported());
        assertEquals(48, persons.linkedRecords());
        assertEquals(30, persons.affectedRecords());
        assertEquals(42, persons.openIssues()); // 50 - 8 activity issues
        assertEquals(88.0, persons.averageScore());

        var units = row(rows, RelatedEntityType.ORGANISATION_UNITS);
        assertTrue(units.supported());
        assertEquals(2, units.linkedRecords());
        assertEquals(2, units.affectedRecords());
        assertEquals(3, units.openIssues());
        assertEquals(75.0, units.averageScore());
    }

    /**
     * A person whose only failures are seven occurrences of two involvement rules has no
     * person-level issues, and the issues list shows those seven as two rows.
     */
    @Test
    public void shouldSubtractActivityOccurrencesButDisplayDistinctActivityRules() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ORGANISATION_UNIT_ENTITY_TYPE, 1))
            .thenReturn(
                Optional.of(revisionWithProfiles(ORGANISATION_UNIT_ENTITY_TYPE, "PTCRIS")));
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(1))
            .thenReturn(List.of(1));
        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()),
                Optional.of(new DataQualityAggregator.AssessmentAggregates(
                    1, 7, 4, 2, 7, 4, 389.9, 1, 96.7)),
                Optional.of(DataQualityAggregator.AssessmentAggregates.empty()));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(new DataQualityAggregator.LinkedDocumentAggregates(0, 0)));

        // when
        var rows = dataQualityService.getRelatedQualityForEntity(ORGANISATION_UNIT_ENTITY_TYPE, 1)
            .getFirst().relatedQuality();

        // then
        assertEquals(0, row(rows, RelatedEntityType.PERSONS).openIssues());
        assertEquals(2, row(rows, RelatedEntityType.ACTIVITIES).openIssues());
    }

    @Test
    public void shouldRelateAPersonToItsEmploymentInstitutionsButNotToOtherPersons() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        var personIndex = new PersonIndex();
        personIndex.setEmploymentInstitutionsId(List.of(5, 6));
        when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.of(personIndex));

        stubAggregates(2, 5, 4, 1, 92.0, 186, 5);
        when(dataQualityAggregator.countRecords(eq("organisation_unit"), any())).thenReturn(2L);

        // when
        var rows = dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality();

        // then
        assertFalse(row(rows, RelatedEntityType.PERSONS).supported());

        var units = row(rows, RelatedEntityType.ORGANISATION_UNITS);
        assertTrue(units.supported());
        assertEquals(2, units.linkedRecords());

        var unitQuery = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator).countRecords(eq("organisation_unit"), unitQuery.capture());
        assertTrue(unitQuery.getValue().toString().contains("databaseId"));
    }

    @Test
    public void shouldReportEmptyUnitsForAPersonWithoutEmployments() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));
        when(personIndexRepository.findByDatabaseId(1)).thenReturn(Optional.empty());

        stubAggregates(2, 5, 4, 1, 92.0, 186, 5);

        // when
        var units = row(dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality(), RelatedEntityType.ORGANISATION_UNITS);

        // then
        assertEquals(0, units.linkedRecords());
        assertNull(units.averageScore());
        verify(dataQualityAggregator, never()).countRecords(eq("organisation_unit"), any());
    }

    @Test
    public void shouldReturnUnsupportedRowsForProjectsAndFundings() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            PERSON_ENTITY_TYPE, 1))
            .thenReturn(Optional.of(revisionWithProfiles(PERSON_ENTITY_TYPE, "PTCRIS")));

        stubAggregates(1, 1, 0, 0, 90.0, 1, 0);

        // when
        var relatedQuality = dataQualityService.getRelatedQualityForEntity(PERSON_ENTITY_TYPE, 1)
            .getFirst().relatedQuality();

        // then (the rows come in the order the analytics table uses)
        assertEquals(List.of(RelatedEntityType.PERSONS, RelatedEntityType.ORGANISATION_UNITS,
                RelatedEntityType.OUTPUTS, RelatedEntityType.ACTIVITIES,
                RelatedEntityType.PROJECTS, RelatedEntityType.FUNDINGS),
            relatedQuality.stream().map(RelatedQualityDTO::entityType).toList());

        assertTrue(row(relatedQuality, RelatedEntityType.ACTIVITIES).supported());

        List.of(row(relatedQuality, RelatedEntityType.PROJECTS),
            row(relatedQuality, RelatedEntityType.FUNDINGS)).forEach(row -> {
            assertFalse(row.supported());
            assertEquals(0, row.linkedRecords());
            assertEquals(0, row.affectedRecords());
            assertEquals(0, row.openIssues());
            assertNull(row.averageScore());
        });
    }

    @Test
    public void shouldDropAssessmentsAndPublishReassessmentEventForLatestRevision() {
        // given
        var revision = revisionWithProfiles(ENTITY_TYPE, "PTCRIS");
        revision.setCompressedContent(CompressionUtil.compress("{\"id\":1}"));
        revision.getAssessments().getFirst().setId(7);

        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revision));

        // when
        var result = dataQualityService.reassessLatestRevision(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertTrue(result);
        assertTrue(revision.getAssessments().isEmpty());

        verify(dataQualityAssessmentIndexRepository).deleteById("7");
        verify(entityRevisionRepository).save(revision);

        var captor = ArgumentCaptor.forClass(DataQualityAssessmentEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertEquals(revision, captor.getValue().entityRevision());
        assertEquals("{\"id\":1}", captor.getValue().json());
    }

    @Test
    public void shouldDropOnlyTheRequestedProfileWhenReassessing() {
        // given (assessments of the other profiles describe the same revision and stay valid)
        var revision = revisionWithProfiles(ENTITY_TYPE, "PTCRIS", "ZENODO");
        revision.setCompressedContent(CompressionUtil.compress("{\"id\":1}"));
        revision.getAssessments().getFirst().setId(7);
        revision.getAssessments().get(1).setId(8);

        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revision));

        // when
        var result = dataQualityService.reassessLatestRevision(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertTrue(result);
        assertEquals(1, revision.getAssessments().size());
        assertEquals("ZENODO", revision.getAssessments().getFirst().getProfileName());

        verify(dataQualityAssessmentIndexRepository).deleteById("7");
        verify(dataQualityAssessmentIndexRepository, never()).deleteById("8");

        var captor = ArgumentCaptor.forClass(DataQualityAssessmentEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        assertEquals("PTCRIS", captor.getValue().profileName());
    }

    @Test
    public void shouldDropEveryProfileWhenNoneIsRequested() {
        // given
        var revision = revisionWithProfiles(ENTITY_TYPE, "PTCRIS", "ZENODO");
        revision.setCompressedContent(CompressionUtil.compress("{\"id\":1}"));
        revision.getAssessments().getFirst().setId(7);
        revision.getAssessments().get(1).setId(8);

        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.of(revision));

        // when
        var result = dataQualityService.reassessLatestRevision(ENTITY_TYPE, 1, null);

        // then
        assertTrue(result);
        assertTrue(revision.getAssessments().isEmpty());

        verify(dataQualityAssessmentIndexRepository).deleteById("7");
        verify(dataQualityAssessmentIndexRepository).deleteById("8");
    }

    @Test
    public void shouldNotPublishReassessmentEventWhenEntityHasNoRevisions() {
        // given
        when(entityRevisionRepository.findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(
            ENTITY_TYPE, 1)).thenReturn(Optional.empty());

        // when
        var result = dataQualityService.reassessLatestRevision(ENTITY_TYPE, 1, "PTCRIS");

        // then
        assertFalse(result);

        verify(entityRevisionRepository, never()).save(any());
        verifyNoInteractions(applicationEventPublisher);
        verifyNoInteractions(dataQualityAssessmentIndexRepository);
    }

    private DataQualityAssessmentConfigurationLoader.DataQualityRemark remarkForTarget(
        String target, IssueSeverity severity, QualityDimension dimension, boolean blocking,
        boolean fairRelated) {
        return new DataQualityAssessmentConfigurationLoader.DataQualityRemark(
            Map.of("en", "Resolvable ORCID"), Map.of("en", "The ORCID could not be resolved."),
            target, severity, dimension, blocking, 8.0, fairRelated, Map.of());
    }

    private ConstraintEvaluationResult occurrence(String ruleKey, String... parameters) {
        return ConstraintEvaluationResult.builder()
            .key(ruleKey)
            .parameters(List.of(parameters))
            .build();
    }

    private DataQualityAssessment assessmentWithIssues(Integer assessmentId,
                                                       List<ConstraintEvaluationResult> issues) {
        var revision = EntityRevision.builder()
            .entityType(PERSON_ENTITY_TYPE)
            .entityId(11)
            .majorVersion(4)
            .minorVersion(2)
            .revisionTimestamp(Instant.now())
            .build();

        var assessment = DataQualityAssessment.builder()
            .revision(revision)
            .profileName("PTCRIS")
            .profileVersion("1.3")
            .startedAt(Instant.now())
            .finishedAt(Instant.now())
            .issues(issues)
            .build();

        assessment.setId(assessmentId);
        revision.getAssessments().add(assessment);

        return assessment;
    }

    private void stubIssueConfiguration(
        MockedStatic<DataQualityAssessmentConfigurationLoader> configurationLoader,
        DataQualityAssessmentConfigurationLoader.DataQualityRemark remark) {
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getIssue(
                anyString(), anyString(), anyString()))
            .thenReturn(remark);
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getWeightedPoints(
                anyString(), anyString(), any()))
            .thenReturn(24.0);
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getTargetWeights(
                anyString(), anyString()))
            .thenReturn(Map.of(remark.target(), 3.0));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                anyString(), anyString(), anyString()))
            .thenReturn(Set.of(multilingualContent("Resolvable ORCID")));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                anyString(), anyString(), anyString(), any()))
            .thenReturn(Set.of(multilingualContent("The ORCID could not be resolved.")));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDimensionDefinition(
                anyString(), anyString(), any()))
            .thenReturn(Set.of(multilingualContent("This dimension ensures accuracy.")));
    }

    @Test
    public void shouldReturnDetailsOfFailedRule() {
        // given
        var assessment = assessmentWithIssues(7,
            List.of(occurrence("orcidNotResolvable", "0000-0002-1847-302X")));

        when(dataQualityAssessmentRepository.findWithRevisionById(7))
            .thenReturn(Optional.of(assessment));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader,
                remarkForTarget("Person.ORCID", IssueSeverity.ERROR, QualityDimension.ACCURACY,
                    true, true));

            // when
            var result = dataQualityService.findIssueDetails(7, "orcidNotResolvable");

            // then
            assertEquals(7, result.assessmentId());
            assertEquals("orcidNotResolvable", result.ruleKey());
            assertEquals(PERSON_ENTITY_TYPE, result.entityType());
            assertEquals(11, result.entityId());
            assertEquals(4, result.recordMajorVersion());
            assertEquals(2, result.recordMinorVersion());
            assertEquals(assessment.getFinishedAt(), result.assessmentDate());

            // A failed constraint always scores zero of what it was worth.
            assertEquals(0.0, result.score());
            assertEquals(24.0, result.maximumScore());

            assertEquals(IssueSeverity.ERROR, result.severity());
            assertEquals(QualityDimension.ACCURACY, result.dimension());
            assertEquals("Person", result.targetEntityType());
            assertEquals("Person.ORCID", result.targetObject());
            assertEquals(3.0, result.constraintWeight());
            assertTrue(result.fairRelated());
            assertTrue(result.blocking());
            assertEquals("PTCRIS", result.policy());
            assertEquals("1.3", result.policyVersion());

            assertEquals("Resolvable ORCID", result.title().getFirst().getContent());
            assertEquals("This dimension ensures accuracy.",
                result.dimensionDefinition().getFirst().getContent());

            assertEquals(1, result.occurrences().size());
            assertEquals(List.of("0000-0002-1847-302X"),
                result.occurrences().getFirst().actualValue());
            assertEquals("The ORCID could not be resolved.",
                result.occurrences().getFirst().message().getFirst().getContent());
        }
    }

    @Test
    public void shouldReturnEveryOccurrenceRecordedUnderTheSameRuleKey() {
        // given (a rule checked once per contributor fails once per offending value)
        var assessment = assessmentWithIssues(7, List.of(
            occurrence("orcidNotResolvable", "0000-0002-1847-302X"),
            occurrence("titleMissing"),
            occurrence("orcidNotResolvable", "0000-0001-0000-111X"),
            occurrence("orcidNotResolvable", "0000-0003-9999-222X")));

        when(dataQualityAssessmentRepository.findWithRevisionById(7))
            .thenReturn(Optional.of(assessment));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader,
                remarkForTarget("Person.ORCID", IssueSeverity.ERROR, QualityDimension.ACCURACY,
                    false, false));

            // when
            var result = dataQualityService.findIssueDetails(7, "orcidNotResolvable");

            // then
            assertEquals(3, result.occurrences().size());
            assertEquals(List.of("0000-0002-1847-302X"),
                result.occurrences().getFirst().actualValue());
            assertEquals(List.of("0000-0001-0000-111X"),
                result.occurrences().get(1).actualValue());
            assertEquals(List.of("0000-0003-9999-222X"),
                result.occurrences().get(2).actualValue());

            assertFalse(result.fairRelated());
            assertFalse(result.blocking());
        }
    }

    @Test
    public void shouldReturnTargetObjectAsEntityTypeWhenTargetHasNoField() {
        // given
        var assessment = assessmentWithIssues(7, List.of(occurrence("contactMissing")));

        when(dataQualityAssessmentRepository.findWithRevisionById(7))
            .thenReturn(Optional.of(assessment));

        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            stubIssueConfiguration(configurationLoader,
                remarkForTarget("Contact", IssueSeverity.WARNING, QualityDimension.CONSISTENCY,
                    false, false));

            // when
            var result = dataQualityService.findIssueDetails(7, "contactMissing");

            // then
            assertEquals("Contact", result.targetEntityType());
            assertEquals("Contact", result.targetObject());
        }
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenAssessmentDoesNotExist() {
        // given
        when(dataQualityAssessmentRepository.findWithRevisionById(9))
            .thenReturn(Optional.empty());

        // when
        assertThrows(NotFoundException.class,
            () -> dataQualityService.findIssueDetails(9, "orcidNotResolvable"));

        // then (NotFoundException should be thrown)
        verify(dataQualityAssessmentRepository).findWithRevisionById(9);
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenAssessmentRecordsNoFailureOfThatRule() {
        // given
        var assessment = assessmentWithIssues(7, List.of(occurrence("titleMissing")));

        when(dataQualityAssessmentRepository.findWithRevisionById(7))
            .thenReturn(Optional.of(assessment));

        // when
        assertThrows(NotFoundException.class,
            () -> dataQualityService.findIssueDetails(7, "orcidNotResolvable"));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldThrowNotFoundExceptionWhenAssessmentHasNoIssuesAtAll() {
        // given
        var assessment = assessmentWithIssues(7, null);

        when(dataQualityAssessmentRepository.findWithRevisionById(7))
            .thenReturn(Optional.of(assessment));

        // when
        assertThrows(NotFoundException.class,
            () -> dataQualityService.findIssueDetails(7, "orcidNotResolvable"));

        // then (NotFoundException should be thrown)
    }

    @Test
    public void shouldListConstraintsOfTheRequestedTarget() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(
                    "ptcris"))
                .thenReturn("1.0.0");
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    "ptcris", "1.0.0", "Document", null, null))
                .thenReturn(new LinkedHashSet<>(List.of("titleTooLong", "doiNotResolvable")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));

            // when
            var result = dataQualityService.listProfileConstraints("ptcris", "Document");

            // then
            assertEquals(2, result.size());

            // Sorted by key, so the picker is stable regardless of profile order.
            assertEquals("doiNotResolvable", result.getFirst().key());
            assertEquals("titleTooLong", result.get(1).key());

            assertEquals("Title", result.getFirst().title().getFirst().getContent());
        }
    }

    @Test
    public void shouldListEveryConstraintWhenNoTargetIsRequested() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(
                    "ptcris"))
                .thenReturn("1.0.0");
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    "ptcris", "1.0.0", null, null, null))
                .thenReturn(new LinkedHashSet<>(List.of("titleTooLong")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));

            // when
            var result = dataQualityService.listProfileConstraints("ptcris", null);

            // then
            assertEquals(1, result.size());
            assertEquals("titleTooLong", result.getFirst().key());
        }
    }

    @Test
    public void shouldReturnEmptyConstraintListWhenTargetHasNoRules() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(
                    "ptcris"))
                .thenReturn("1.0.0");
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>());

            // when
            var result = dataQualityService.listProfileConstraints("ptcris", "Funding");

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void shouldListConstraintsOfTheLatestProfileVersion() {
        // given (a profile with an older version still configured alongside the current one)
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(
                    "ptcris"))
                .thenReturn("2.0.0");
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    anyString(), anyString(), any(), any(), any()))
                .thenReturn(new LinkedHashSet<>(List.of("titleTooLong")));
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Title")));

            // when
            dataQualityService.listProfileConstraints("ptcris", "Document");

            // then (the picker must describe the rules currently in force)
            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    "ptcris", "2.0.0", "Document", null, null));
            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    "ptcris", "2.0.0", "titleTooLong"));
        }
    }

    @Test
    public void shouldListProfileNamesWithTheirLatestVersion() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
                .thenReturn(new LinkedHashSet<>(List.of(
                    new Pair<>("ptcris", "1.0.0"),
                    new Pair<>("zenodo", "2.1.0"))));

            // when
            var result = dataQualityService.listDataQualityProfileNames();

            // then
            assertEquals(2, result.size());
            assertEquals("ptcris", result.getFirst().profileName());
            assertEquals("1.0.0", result.getFirst().version());
            assertEquals("zenodo", result.get(1).profileName());
            assertEquals("2.1.0", result.get(1).version());
        }
    }

    @Test
    public void shouldListProfileNamesWithoutTouchingTheDatabase() {
        // given (the full listing resolves a language tag per translated string of every rule,
        // which is exactly what the pickers must avoid)
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
                .thenReturn(new LinkedHashSet<>(List.of(new Pair<>("ptcris", "1.0.0"))));

            // when
            dataQualityService.listDataQualityProfileNames();

            // then
            verifyNoInteractions(languageTagService);

            configurationLoader.verify(
                () -> DataQualityAssessmentConfigurationLoader.getProfile(anyString(), anyString()),
                never());
        }
    }

    private DataQualityAssessmentConfigurationLoader.DataQualityRemark remarkFor(
        String target, IssueSeverity severity, QualityDimension dimension) {
        return new DataQualityAssessmentConfigurationLoader.DataQualityRemark(
            Map.of("EN", "Title"), Map.of("EN", "Message"), target, severity, dimension, true,
            8.0, false, Map.of("maxLength", 255));
    }

    private DataQualityAssessmentConfigurationLoader.DataQualityProfile profileWith(
        Map<String, DataQualityAssessmentConfigurationLoader.DataQualityRemark> remarks) {
        return new DataQualityAssessmentConfigurationLoader.DataQualityProfile(
            "1.0.0", 60.0, Map.of(), Map.of("Document.title", 5.0), remarks, Map.of(), Map.of(),
            Map.of());
    }

    private MockedStatic<DataQualityAssessmentConfigurationLoader> mockPolicyLoader(
        Map<String, DataQualityAssessmentConfigurationLoader.DataQualityRemark> remarks) {
        var configurationLoader = mockStatic(DataQualityAssessmentConfigurationLoader.class);

        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion("ptcris"))
            .thenReturn("1.0.0");
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getProfile("ptcris", "1.0.0"))
            .thenReturn(profileWith(remarks));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getDimensionDefinition(
                anyString(), anyString(), any()))
            .thenReturn(Set.of(multilingualContent("Definition")));

        return configurationLoader;
    }

    @Test
    public void shouldReturnEveryConstraintOfThePolicyWithItsAffectedRecordCount() {
        // given
        var remarks = new LinkedHashMap<String,
            DataQualityAssessmentConfigurationLoader.DataQualityRemark>();
        remarks.put("titleTooLong",
            remarkFor("Document.title", IssueSeverity.ERROR, QualityDimension.CONSISTENCY));
        remarks.put("doiNotResolvable",
            remarkFor("Document.doi", IssueSeverity.WARNING, QualityDimension.QUALITATIVE));

        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt()))
            .thenReturn(List.of(new DataQualityAggregator.TopFailedRule("doiNotResolvable", 4281)));

        try (var ignored = mockPolicyLoader(remarks)) {
            // when
            var policy = dataQualityService.getPolicy(null, "ptcris", null);

            // then
            assertEquals("ptcris", policy.profileName());
            assertEquals("1.0.0", policy.version());
            assertEquals(2, policy.constraints().size());

            var top = policy.constraints().getFirst();
            assertEquals("doiNotResolvable", top.key());
            assertEquals(4281, top.affectedRecords());
            assertEquals(IssueSeverity.WARNING, top.severity());
            assertEquals(QualityDimension.QUALITATIVE, top.dimension());

            // Rules nothing failed are missing from the buckets and fall back to zero.
            assertEquals("titleTooLong", policy.constraints().get(1).key());
            assertEquals(0, policy.constraints().get(1).affectedRecords());
        }
    }

    @Test
    public void shouldCarryRuleMetadataAndTargetWeightIntoThePolicy() {
        // given
        var remarks = Map.of("titleTooLong",
            remarkFor("Document.title", IssueSeverity.ERROR, QualityDimension.CONSISTENCY));

        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt())).thenReturn(List.of());

        try (var ignored = mockPolicyLoader(remarks)) {
            // when
            var constraint = dataQualityService.getPolicy(null, "ptcris", null)
                .constraints().getFirst();

            // then
            assertEquals("Document.title", constraint.target());
            assertEquals(5.0, constraint.targetWeight());
            assertEquals(8.0, constraint.points());
            assertTrue(constraint.blocking());
            assertFalse(constraint.usedForFairCompliance());
            assertEquals(255, constraint.constraints().get("maxLength"));
        }
    }

    @Test
    public void shouldDescribeEveryDimensionOfThePolicy() {
        // given
        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt())).thenReturn(List.of());

        try (var ignored = mockPolicyLoader(Map.of())) {
            // when
            var definitions =
                dataQualityService.getPolicy(null, "ptcris", null).dimensionDefinitions();

            // then
            assertEquals(QualityDimension.values().length, definitions.size());
            assertEquals("Definition",
                definitions.get(QualityDimension.LINEAGE).getFirst().getContent());
        }
    }

    @Test
    public void shouldCountPolicyFailuresOnlyWithinTheRequestedUnitAndDay() {
        // given
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8));
        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt())).thenReturn(List.of());

        try (var ignored = mockPolicyLoader(Map.of())) {
            // when
            dataQualityService.getPolicy(7, "ptcris", LocalDate.of(2026, 7, 18));

            // then
            var captor = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator).topFailedRules(captor.capture(), any(), anyInt());

            var query = captor.getValue().toString();
            assertTrue(query.contains("organisation_unit_ids"));
            assertTrue(query.contains("8"));
            assertTrue(query.contains("assessment_date"));
            assertFalse(query.contains("is_latest"));
        }
    }

    @Test
    public void shouldCountPolicyFailuresAgainstTheLatestAssessmentsWhenNoDayIsGiven() {
        // given
        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt())).thenReturn(List.of());

        try (var ignored = mockPolicyLoader(Map.of())) {
            // when
            dataQualityService.getPolicy(null, "ptcris", null);

            // then
            var captor = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator).topFailedRules(captor.capture(), any(), anyInt());

            var query = captor.getValue().toString();
            assertTrue(query.contains("is_latest"));
            assertFalse(query.contains("organisation_unit_ids"));
        }
    }

    @Test
    public void shouldReturnEmptyProfileNameListWhenNoProfilesAreConfigured() {
        // given
        try (var configurationLoader = mockStatic(
            DataQualityAssessmentConfigurationLoader.class)) {

            configurationLoader
                .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
                .thenReturn(new LinkedHashSet<>());

            // when
            var result = dataQualityService.listDataQualityProfileNames();

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
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
            "1.3", 70.0, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
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
