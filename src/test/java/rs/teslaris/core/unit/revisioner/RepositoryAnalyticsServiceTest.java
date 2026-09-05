package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import rs.teslaris.core.model.commontypes.LanguageTag;
import rs.teslaris.core.model.commontypes.MultiLingualContent;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.service.impl.RepositoryAnalyticsServiceImpl;
import rs.teslaris.revisioner.util.dataquality.DataQualityAggregator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;
import rs.teslaris.revisioner.util.dataquality.TrendGranularity;
import rs.teslaris.revisioner.util.dataquality.TrendMetric;

@SpringBootTest
public class RepositoryAnalyticsServiceTest {

    private static final String PROFILE = "PTCRIS";

    @Mock
    private DataQualityAggregator dataQualityAggregator;

    @Mock
    private OrganisationUnitService organisationUnitService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private RepositoryAnalyticsServiceImpl repositoryAnalyticsService;

    private static DataQualityAggregator.PeriodMetric periodMetric(long records, Double average) {
        return new DataQualityAggregator.PeriodMetric(records, average, 0, 0, 0);
    }

    private MockedStatic<DataQualityAssessmentConfigurationLoader> mockConfigurationLoader() {
        var configurationLoader = mockStatic(DataQualityAssessmentConfigurationLoader.class);

        configurationLoader
            .when(DataQualityAssessmentConfigurationLoader::listAvailableProfilesWithVersion)
            .thenReturn(new LinkedHashSet<>(List.of(new Pair<>(PROFILE, "1.0.0"))));
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                anyString(), anyString(), any(), any(), any()))
            .thenReturn(new LinkedHashSet<>(List.of("activityEndDateMissing")));

        // Without this the mocked static hands back null, and the version-taking stubs below stop
        // matching, because anyString() does not match null.
        configurationLoader
            .when(() -> DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(
                anyString()))
            .thenReturn("1.0.0");

        return configurationLoader;
    }

    private void stubAggregates(long affectedRecords, long openIssues, long activitiesCount,
                                long activityIssues, long publicationCandidates,
                                Double averageScore, long linkedRecords, long linkedActivities) {
        stubAggregates(affectedRecords, openIssues, activitiesCount, activityIssues,
            publicationCandidates, averageScore, linkedRecords, linkedActivities, 0, 0.0);
    }

    private void stubAggregates(long affectedRecords, long openIssues, long activitiesCount,
                                long activityIssues, long publicationCandidates,
                                Double averageScore, long linkedRecords, long linkedActivities,
                                long activityCandidates, double activityScoreSum) {
        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(Optional.of(new DataQualityAggregator.AssessmentAggregates(
                affectedRecords, openIssues, activitiesCount, activityIssues,
                activityCandidates, activityScoreSum, publicationCandidates, averageScore)));
        when(dataQualityAggregator.aggregateLinkedDocuments(any()))
            .thenReturn(Optional.of(new DataQualityAggregator.LinkedDocumentAggregates(
                linkedRecords, linkedActivities)));
    }

    private String capturedAssessmentQuery() {
        var captor = ArgumentCaptor.forClass(Query.class);
        verify(dataQualityAggregator, atLeastOnce())
            .aggregateAssessments(captor.capture(), any());

        return captor.getValue().toString();
    }

    private MultiLingualContent multilingualContent(String content) {
        var languageTag = new LanguageTag();
        languageTag.setId(1);
        languageTag.setLanguageTag("EN");

        return new MultiLingualContent(languageTag, content, 1);
    }

    private void stubTopFailedRule(String ruleKey, long occurrences) {
        when(dataQualityAggregator.topFailedRule(any(), any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.TopFailedRule(ruleKey, occurrences)));
    }

    /**
     * The same breakdown answers every scope pass - persons, organisation units and outputs - so a
     * figure a row derives from more than one pass shows up multiplied in the assertions.
     */
    private void stubIssueBreakdown(long errorIssues, long warningIssues, long infoIssues,
                                    long activityErrorIssues, long activityWarningIssues,
                                    long activityInfoIssues) {
        when(dataQualityAggregator.aggregateIssueBreakdown(any()))
            .thenReturn(Optional.of(new DataQualityAggregator.IssueBreakdown(
                errorIssues, warningIssues, infoIssues, activityErrorIssues,
                activityWarningIssues, activityInfoIssues)));
    }

    private void stubBlocking(long distinctConstraints, long blockingIssues) {
        when(dataQualityAggregator.aggregateBlocking(any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.BlockingAggregates(distinctConstraints, blockingIssues)));
    }

    @Test
    public void shouldSummarisePublicationCandidatesAcrossTheRepository() {
        // given
        stubAggregates(1483281, 18426, 30, 8, 1278610, 84.7, 1000, 300);
        stubBlocking(14, 204671);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var analysis = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null);

            // then
            assertEquals(1278610, analysis.publicationCandidates());

            // Everything assessed that is not a candidate.
            assertEquals(204671, analysis.notPublicationCandidates());
            assertEquals(86.2, analysis.candidateRate(), 0.05);

            assertEquals(14, analysis.blockingConstraints());
            assertEquals(204671, analysis.blockingIssues());
        }
    }

    @Test
    public void shouldCarryTheCandidateRateOfEveryEntityTypeIntoTheAnalysis() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubBlocking(3, 40);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var analysis = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null);

            // then
            assertEquals(6, analysis.candidateRateByEntityType().size());
            assertEquals(90.0,
                analysis.candidateRateByEntityType().getFirst().publicationCandidatePercentage(),
                0.0001);
        }
    }

    @Test
    public void shouldReportTheMostFrequentBlockingConstraintPerEntityType() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubBlocking(5, 60);
        when(dataQualityAggregator.topFailedRule(any(), any(), anyString()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.TopFailedRule("titleMissing", 986)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var constraints = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null).mostCommonBlockingConstraints();

            // then
            assertEquals(6, constraints.size());
            assertEquals("titleMissing", constraints.getFirst().ruleKey());
            assertEquals(986, constraints.getFirst().occurrences());

            // Only blocking failures can keep a record from being a candidate, so the aggregation
            // reads the blocking key field rather than every failed rule.
            var field = ArgumentCaptor.forClass(String.class);
            verify(dataQualityAggregator, times(3))
                .topFailedRule(any(), any(), field.capture());
            assertEquals("blocking_rule_keys", field.getValue());

            // Activities are counted per activity, so that row sums the occurrence counters
            // instead of aggregating a distinct-key list.
            verify(dataQualityAggregator).topRuleByActivityOccurrences(any(), any());
        }
    }

    @Test
    public void shouldReportNoBlockingConstraintForEntityTypesWithoutOne() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubBlocking(0, 0);
        when(dataQualityAggregator.topFailedRule(any(), any(), anyString()))
            .thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var constraints = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null).mostCommonBlockingConstraints();

            // then
            constraints.forEach(constraint -> {
                assertNull(constraint.ruleKey());
                assertEquals(0, constraint.occurrences());
            });
        }
    }

    @Test
    public void shouldReportNoCandidateRateWhenNothingIsAssessed() {
        // given
        stubAggregates(0, 0, 0, 0, 0, null, 0, 0);
        stubBlocking(0, 0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var analysis = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null);

            // then
            assertNull(analysis.candidateRate());
            assertEquals(0, analysis.publicationCandidates());
            assertEquals(0, analysis.notPublicationCandidates());
        }
    }

    @Test
    public void shouldFallBackToEmptyBlockingFiguresWhenAggregationIsUnavailable() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        when(dataQualityAggregator.aggregateBlocking(any())).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var analysis = repositoryAnalyticsService.getPublicationCandidateAnalysis(
                PROFILE, null, null);

            // then
            assertEquals(0, analysis.blockingConstraints());
            assertEquals(0, analysis.blockingIssues());
            assertEquals(45, analysis.publicationCandidates());
        }
    }

    @Test
    public void shouldSummariseTheWholeRepositoryRegardlessOfEntityType() {
        // given
        stubAggregates(1483281, 18426, 30, 8, 1278000, 84.7, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var overview = repositoryAnalyticsService.getOverview(PROFILE, null, null);

            // then
            assertEquals(84.7, overview.averageScore());
            assertEquals(18426, overview.openIssues());
            assertEquals(1483281, overview.recordsAssessed());

            // 1278000 of 1483281 assessed records are publication candidates.
            assertEquals(86.2, overview.publicationCandidatePercentage(), 0.05);
        }
    }

    @Test
    public void shouldNotNarrowTheEntityTypeOfTheSummary() {
        // given
        stubAggregates(10, 5, 0, 0, 5, 90.0, 10, 0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getOverview(PROFILE, null, null);

            // then (the first aggregation is the summary, and it spans every kind of record)
            var captor = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator, atLeastOnce())
                .aggregateAssessments(captor.capture(), any());

            assertFalse(captor.getAllValues().getFirst().toString().contains("entity_type"));
        }
    }

    @Test
    public void shouldCarryTheEntityTypeTableIntoTheOverview() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var overview = repositoryAnalyticsService.getOverview(PROFILE, null, null);

            // then
            assertEquals(6, overview.qualityByEntityType().size());
            assertEquals(RepositoryEntityType.PERSONS,
                overview.qualityByEntityType().getFirst().entityType());
        }
    }

    @Test
    public void shouldReportTheMostFrequentIssueOfEveryEntityType() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubTopFailedRule("orcidNotResolvable", 4281);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var issues = repositoryAnalyticsService.getOverview(PROFILE, null, null)
                .issuesRequiringAttention();

            // then
            assertEquals(6, issues.size());
            assertEquals(RepositoryEntityType.PERSONS, issues.getFirst().entityType());
            assertEquals("orcidNotResolvable", issues.getFirst().ruleKey());
            assertEquals(4281, issues.getFirst().occurrences());

            // The rules of each family are what the aggregation is restricted to. Activities are
            // not among them - that row counts occurrences rather than records.
            verify(dataQualityAggregator, times(3)).topFailedRule(any(), any());
            verify(dataQualityAggregator).topRuleByActivityOccurrences(any(), any());
        }
    }

    /**
     * A document raises one activity issue per offending contribution, so the row has to report the
     * activities a rule affected rather than the records it failed on.
     */
    @Test
    public void shouldReportActivityIssuesByOccurrenceRatherThanByRecord() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubTopFailedRule("orcidNotResolvable", 4281);

        when(dataQualityAggregator.topRuleByActivityOccurrences(any(), any()))
            .thenReturn(Optional.of(
                new DataQualityAggregator.TopFailedRule("activityEndDateMissing", 91204)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var activities = repositoryAnalyticsService.getOverview(PROFILE, null, null)
                .issuesRequiringAttention().get(3);

            // then
            assertEquals(RepositoryEntityType.ACTIVITIES, activities.entityType());
            assertEquals("activityEndDateMissing", activities.ruleKey());
            assertEquals(91204, activities.occurrences());
        }
    }

    @Test
    public void shouldReportNoIssueForEntityTypesWithoutAssessments() {
        // given (nothing has failed, or the type carries no assessments at all)
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        when(dataQualityAggregator.topFailedRule(any(), any())).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var issues = repositoryAnalyticsService.getOverview(PROFILE, null, null)
                .issuesRequiringAttention();

            // then
            issues.forEach(issue -> {
                assertNull(issue.ruleKey());
                assertEquals(0, issue.occurrences());
                assertTrue(issue.title().isEmpty());
            });

            // Projects and fundings are never even queried.
            assertEquals(RepositoryEntityType.PROJECTS, issues.get(4).entityType());
            assertEquals(RepositoryEntityType.FUNDINGS, issues.get(5).entityType());
        }
    }

    @Test
    public void shouldScopeTheOverviewToTheOrganisationUnitSubHierarchy() {
        // given
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8));

        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);
        stubTopFailedRule("orcidNotResolvable", 12);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getOverview(PROFILE, 7, LocalDate.of(2026, 7, 18));

            // then
            var captor = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator, atLeastOnce()).topFailedRule(captor.capture(), any());

            var query = captor.getValue().toString();
            assertTrue(query.contains("organisation_unit_ids"));
            assertTrue(query.contains("8"));
            assertTrue(query.contains("2026-07-18T23:59:59.999"));
        }
    }

    @Test
    public void shouldReturnOneRowPerEntityType() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null);

            // then
            assertEquals(6, result.size());
            assertEquals(RepositoryEntityType.PERSONS, result.get(0).entityType());
            assertEquals(RepositoryEntityType.ORGANISATION_UNITS, result.get(1).entityType());
            assertEquals(RepositoryEntityType.OUTPUTS, result.get(2).entityType());
            assertEquals(RepositoryEntityType.ACTIVITIES, result.get(3).entityType());
            assertEquals(RepositoryEntityType.PROJECTS, result.get(4).entityType());
            assertEquals(RepositoryEntityType.FUNDINGS, result.get(5).entityType());
        }
    }

    @Test
    public void shouldMarkProjectsAndFundingsUnsupported() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null);

            // then
            List.of(result.get(4), result.get(5)).forEach(row -> {
                assertFalse(row.supported());
                assertEquals(0, row.records());
                assertEquals(0, row.affectedRecords());
                assertEquals(0, row.openIssues());
                assertNull(row.averageScore());
                assertNull(row.publicationCandidatePercentage());
            });
        }
    }

    @Test
    public void shouldComputeFiguresOfAssessedEntityTypes() {
        // given
        when(dataQualityAggregator.countRecords(eq("person"), any())).thenReturn(48620L);

        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var persons = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null)
                .getFirst();

            // then
            assertTrue(persons.supported());
            assertEquals(48620, persons.records());
            assertEquals(50, persons.affectedRecords());

            // Activity issues are reported on the Activities row, so they are left out here
            // rather than counted twice in the same table: 120 - 8.
            assertEquals(112, persons.openIssues());
            assertEquals(92.0, persons.averageScore());

            // 45 of the 50 assessed records are publication candidates.
            assertEquals(90.0, persons.publicationCandidatePercentage());
        }
    }

    /**
     * A person's involvements are activities held on the person record, so they are summed from the
     * person index and added to the activities the outputs carry.
     */
    @Test
    public void shouldAddInvolvementActivitiesToTheActivitiesRow() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        when(dataQualityAggregator.sumField(eq("person"), any(), eq("activities_count")))
            .thenReturn(700L);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var activities =
                repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null).get(3);

            // then
            assertEquals(1000, activities.records()); // 300 on outputs + 700 on persons
        }
    }

    @Test
    public void shouldCountOutputsAndActivitiesFromTheSameDocumentAggregation() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1284310, 86204);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null);

            // then
            var outputs = result.get(2);
            assertEquals(1284310, outputs.records());
            assertEquals(50, outputs.affectedRecords());

            // Activities live on outputs and on persons, so their totals are sums of the activity
            // counters of both passes.
            var activities = result.get(3);
            assertEquals(86204, activities.records());
            assertEquals(60, activities.affectedRecords());
            assertEquals(16, activities.openIssues());
            assertTrue(activities.supported());

            verify(dataQualityAggregator).aggregateLinkedDocuments(any());
        }
    }

    /**
     * An activity's score and candidacy are accumulated as sums when its record is assessed, so the
     * row divides them by the number of activities assessed - not by the number of records, which
     * would let a document holding one activity weigh as much as one holding thirty.
     */
    @Test
    public void shouldScoreActivitiesPerActivityRatherThanPerRecord() {
        // given (30 activities per pass, so 60 assessed across outputs and persons)
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300, 12, 2400.0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var activities =
                repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null).get(3);

            // then
            assertEquals(60, activities.affectedRecords());

            // 4800 points over 60 activities, and 24 of them are candidates.
            assertEquals(80.0, activities.averageScore());
            assertEquals(40.0, activities.publicationCandidatePercentage());
        }
    }

    @Test
    public void shouldReportNoActivityScoreWhenNoActivityWasAssessed() {
        // given
        stubAggregates(50, 120, 0, 0, 45, 92.0, 1000, 0, 0, 0.0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var activities =
                repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null).get(3);

            // then
            assertNull(activities.averageScore());
            assertNull(activities.publicationCandidatePercentage());
        }
    }

    @Test
    public void shouldSumIssueSeveritiesOverEveryEntityType() {
        // given (100/50/20 per scope, of which 10 per severity belong to activities)
        stubIssueBreakdown(100, 50, 20, 10, 10, 10);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var statistics = repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null);

            // then (three record rows of 90/40/10 plus an activities row of 20/20/20)
            assertEquals(290, statistics.errorIssues());
            assertEquals(140, statistics.warningIssues());
            assertEquals(50, statistics.infoIssues());
            assertEquals(480, statistics.openIssues());
        }
    }

    /**
     * The severity counters cover the whole record, so an activity issue would otherwise be counted
     * both on the row of the record raising it and on the Activities row.
     */
    @Test
    public void shouldReportActivityIssuesOnTheActivitiesRowOnly() {
        // given
        stubIssueBreakdown(100, 50, 20, 10, 10, 10);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var rows = repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null)
                .issuesBySeverityAndEntityType();

            // then
            assertEquals(6, rows.size());

            var persons = rows.getFirst();
            assertEquals(RepositoryEntityType.PERSONS, persons.entityType());
            assertEquals(90, persons.errorIssues());
            assertEquals(40, persons.warningIssues());
            assertEquals(10, persons.infoIssues());

            // Documents and persons both raise activities, so that row gathers two passes.
            var activities = rows.get(3);
            assertEquals(RepositoryEntityType.ACTIVITIES, activities.entityType());
            assertEquals(20, activities.errorIssues());
            assertEquals(20, activities.warningIssues());
            assertEquals(20, activities.infoIssues());
        }
    }

    @Test
    public void shouldMarkProjectsAndFundingsUnsupportedInTheIssueBreakdown() {
        // given
        stubIssueBreakdown(100, 50, 20, 0, 0, 0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var rows = repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null)
                .issuesBySeverityAndEntityType();

            // then
            List.of(rows.get(4), rows.get(5)).forEach(row -> {
                assertFalse(row.supported());
                assertEquals(0, row.errorIssues());
                assertEquals(0, row.warningIssues());
                assertEquals(0, row.infoIssues());
            });
        }
    }

    /**
     * A recurring constraint stands for a rule across the whole repository rather than for one
     * entity type, so the rows carry no entity type.
     */
    @Test
    public void shouldListTheTopRecurringConstraintsMostFrequentFirst() {
        // given
        stubIssueBreakdown(100, 50, 20, 0, 0, 0);

        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt()))
            .thenReturn(List.of(
                new DataQualityAggregator.TopFailedRule("doiNotResolvable", 4281),
                new DataQualityAggregator.TopFailedRule("noProjectFunding", 2164)));

        try (var configurationLoader = mockConfigurationLoader()) {
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Resolvable DOI")));

            // when
            var constraints = repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null)
                .topRecurringConstraints();

            // then
            assertEquals(2, constraints.size());
            assertEquals("doiNotResolvable", constraints.getFirst().ruleKey());
            assertEquals(4281, constraints.getFirst().occurrences());
            assertNull(constraints.getFirst().entityType());
        }
    }

    @Test
    public void shouldAskForAtMostFiveRecurringConstraints() {
        // given
        stubIssueBreakdown(0, 0, 0, 0, 0, 0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null);

            // then
            var limit = ArgumentCaptor.forClass(Integer.class);
            verify(dataQualityAggregator).topFailedRules(any(), any(), limit.capture());

            assertEquals(5, limit.getValue());
        }
    }

    @Test
    public void shouldFallBackToZeroesWhenTheIssueBreakdownIsUnavailable() {
        // given (the report degrades rather than failing)
        when(dataQualityAggregator.aggregateIssueBreakdown(any()))
            .thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var statistics = repositoryAnalyticsService.getIssueStatistics(PROFILE, null, null);

            // then
            assertEquals(0, statistics.openIssues());
            assertTrue(statistics.topRecurringConstraints().isEmpty());
        }
    }

    @Test
    public void shouldExportIssueStatisticsWithBothPanels() {
        // given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubIssueBreakdown(100, 50, 20, 10, 10, 10);

        when(dataQualityAggregator.topFailedRules(any(), any(), anyInt()))
            .thenReturn(List.of(
                new DataQualityAggregator.TopFailedRule("doiNotResolvable", 4281)));

        try (var configurationLoader = mockConfigurationLoader()) {
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Resolvable DOI")));

            // when
            var rows = exportedRows(repositoryAnalyticsService.exportIssueStatistics(
                PROFILE, null, null, "en"));

            // then
            assertEquals("repositoryAnalytics.issueStatistics", rows.getFirst().getFirst());

            List.of("repositoryAnalytics.issuesBySeverityAndEntityType",
                    "repositoryAnalytics.topRecurringConstraints")
                .forEach(panel -> assertTrue(
                    rows.stream().anyMatch(row -> row.contains(panel))));

            assertTrue(rows.stream().anyMatch(row -> row.contains("Resolvable DOI")));

            // Numeric cells stay numeric, so the error total is a number rather than text.
            assertEquals(290.0,
                rowStartingWith(rows, "repositoryAnalytics.header.errorIssues").get(1));
        }
    }

    @Test
    public void shouldReturnNoPercentageWhenNothingIsAssessed() {
        // given
        stubAggregates(0, 0, 0, 0, 0, null, 10, 0);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var persons = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null)
                .getFirst();

            // then
            assertNull(persons.averageScore());
            assertNull(persons.publicationCandidatePercentage());
            assertEquals(0, persons.affectedRecords());
        }
    }

    @Test
    public void shouldFallBackToEmptyFiguresWhenAggregationIsUnavailable() {
        // given (the report degrades rather than failing)
        when(dataQualityAggregator.aggregateAssessments(any(), any())).thenReturn(Optional.empty());
        when(dataQualityAggregator.aggregateLinkedDocuments(any())).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var persons = repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null)
                .getFirst();

            // then
            assertTrue(persons.supported());
            assertEquals(0, persons.affectedRecords());
            assertEquals(0, persons.openIssues());
            assertNull(persons.averageScore());
            assertNull(persons.publicationCandidatePercentage());
        }
    }

    @Test
    public void shouldDescribeCurrentStateWhenNoDateIsRequested() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null);

            // then
            var query = capturedAssessmentQuery();
            assertTrue(query.contains("is_latest"));
            assertFalse(query.contains("valid_to"));
        }
    }

    @Test
    public void shouldResolveRequestedDayToItsLastMoment() {
        // given (the newest assessment of that day is the one that describes it)
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByEntityType(
                PROFILE, null, LocalDate.of(2026, 7, 18));

            // then
            var query = capturedAssessmentQuery();
            assertTrue(query.contains("2026-07-18T23:59:59.999"));
            assertTrue(query.contains("assessment_date"));
            assertTrue(query.contains("valid_to"));
            assertFalse(query.contains("is_latest"));
        }
    }

    @Test
    public void shouldCoverWholeRepositoryWhenNoOrganisationUnitIsRequested() {
        // given
        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByEntityType(PROFILE, null, null);

            // then
            verifyNoInteractions(organisationUnitService);
            assertFalse(capturedAssessmentQuery().contains("organisation_unit_ids"));
        }
    }

    @Test
    public void shouldScopeEveryFigureToTheOrganisationUnitSubHierarchy() {
        // given (a unit is measured against its own records, not the whole repository)
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8, 9));

        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByEntityType(PROFILE, 7, null);

            // then
            var assessmentQuery = capturedAssessmentQuery();
            assertTrue(assessmentQuery.contains("organisation_unit_ids"));
            assertTrue(assessmentQuery.contains("8"));
            assertTrue(assessmentQuery.contains("9"));

            var documentQuery = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator).aggregateLinkedDocuments(documentQuery.capture());
            assertTrue(documentQuery.getValue().toString().contains("organisation_unit_ids"));

            var countQuery = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator).countRecords(eq("person"), countQuery.capture());
            assertTrue(countQuery.getValue().toString().contains("employment_institutions_id"));
        }
    }

    @Test
    public void shouldReturnOnePointPerRequestedPeriod() {
        // given
        stubTrend(Map.of(
            "p0", periodMetric(10, 70.0),
            "p1", periodMetric(12, 75.0),
            "p2", periodMetric(14, 80.0)));

        // when
        var trend = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 3);

        // then
        assertEquals(3, trend.series().size());
        assertEquals(70.0, trend.series().getFirst().value());
        assertEquals(80.0, trend.series().getLast().value());
        assertEquals(14, trend.series().getLast().recordsAssessed());
    }

    @Test
    public void shouldDefaultThePointCountPerGranularity() {
        // given
        stubTrend(Map.of());

        // when
        var daily = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.DAILY, null);
        var weekly = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, null);
        var monthly = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.MONTHLY, null);

        // then
        assertEquals(7, daily.series().size());
        assertEquals(5, weekly.series().size());
        assertEquals(6, monthly.series().size());
    }

    /**
     * The series length drives how many filters one aggregation carries, so an unbounded request
     * would be an unbounded aggregation.
     */
    @Test
    public void shouldClampTheRequestedPointCountToTheHardCap() {
        // given
        stubTrend(Map.of());

        // when
        var tooMany = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.DAILY, 500);
        var tooFew = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.DAILY, 0);

        // then
        assertEquals(10, tooMany.series().size());
        assertEquals(2, tooFew.series().size());
    }

    @Test
    public void shouldMeasureEveryPointInOneAggregation() {
        // given
        stubTrend(Map.of());

        // when
        repositoryAnalyticsService.getQualityTrend(PROFILE, null, TrendMetric.OVERALL_SCORE,
            TrendGranularity.DAILY, 4);

        // then (one request for the series, one for the entity-type panel)
        verify(dataQualityAggregator, times(2)).aggregateMetricByPeriod(any(), any(), any());
        assertEquals(4, capturedPeriodFilters(0).size());
    }

    @Test
    public void shouldSelectTheAssessmentCurrentAtEachPoint() {
        // given
        stubTrend(Map.of());

        // when
        repositoryAnalyticsService.getQualityTrend(PROFILE, null, TrendMetric.OVERALL_SCORE,
            TrendGranularity.DAILY, 2);

        // then
        capturedPeriodFilters(0).values().forEach(filter -> {
            assertTrue(filter.toString().contains("assessment_date"));
            assertTrue(filter.toString().contains("valid_to"));
            assertTrue(filter.toString().contains("23:59:59.999"));
        });
    }

    @Test
    public void shouldDeriveIndicatorsFromTheSeries() {
        // given
        stubTrend(Map.of(
            "p0", periodMetric(10, 72.0),
            "p1", periodMetric(10, 90.0),
            "p2", periodMetric(10, 84.7),
            "p3", periodMetric(10, 82.1)));

        // when
        var indicators = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 4).indicators();

        // then
        assertEquals(82.1, indicators.current());
        assertEquals(84.7, indicators.previous());
        assertEquals(-2.6, indicators.change(), 0.0001);
        assertEquals(90.0, indicators.best());
        assertEquals(72.0, indicators.lowest());
    }

    @Test
    public void shouldReportEmptyIndicatorsWhenNothingWasAssessed() {
        // given
        stubTrend(Map.of());

        // when
        var indicators = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 3).indicators();

        // then
        assertNull(indicators.current());
        assertNull(indicators.change());
        assertNull(indicators.best());
    }

    @Test
    public void shouldAggregateTheScoreFieldOfTheRequestedDimension() {
        // given
        stubTrend(Map.of());

        // when
        repositoryAnalyticsService.getQualityTrend(PROFILE, null, TrendMetric.CONSISTENCY,
            TrendGranularity.WEEKLY, 2);

        // then
        var aggregation = capturedMetricAggregation();
        assertEquals("consistency_score", aggregation.averageField());
        assertEquals("activity_dimension_score_sums.CONSISTENCY", aggregation.activitySumField());
        assertNull(aggregation.matchingField());
    }

    /**
     * A rate is a share of the records in the bucket, not an average of a stored field.
     */
    @Test
    public void shouldCountMatchingRecordsForThePublicationCandidateRate() {
        // given
        stubTrend(Map.of("p0", new DataQualityAggregator.PeriodMetric(200, null, 50, 0, 0)));

        // when
        var trend = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.PUBLICATION_CANDIDATE_RATE, TrendGranularity.WEEKLY, 1);

        // then
        assertEquals("publication_candidate", capturedMetricAggregation().matchingField());
        assertNull(capturedMetricAggregation().averageField());
        assertEquals(25.0, trend.series().getFirst().value());
    }

    @Test
    public void shouldReportEveryEntityTypeInTheTrendPanel() {
        // given
        stubTrend(Map.of());

        // when
        var rows = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 3).trendByEntityType();

        // then
        assertEquals(6, rows.size());
        assertEquals(RepositoryEntityType.PERSONS, rows.getFirst().entityType());
        assertEquals(RepositoryEntityType.ACTIVITIES, rows.get(3).entityType());

        List.of(rows.get(4), rows.get(5)).forEach(row -> {
            assertFalse(row.supported());
            assertNull(row.current());
        });
    }

    /**
     * The panel compares the two newest points, so it needs one filter per entity type per period.
     */
    @Test
    public void shouldCompareTheTwoNewestPointsPerEntityType() {
        // given
        stubTrend(Map.of(
            "p1#PERSONS", periodMetric(10, 92.0),
            "p0#PERSONS", periodMetric(10, 90.6)));

        // when
        var persons = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 5).trendByEntityType().getFirst();

        // then
        assertEquals(8, capturedPeriodFilters(1).size());
        assertEquals(92.0, persons.current());
        assertEquals(90.6, persons.previous());
        assertEquals(1.4, persons.change(), 0.0001);
    }

    /**
     * Activities are not records, so their figure is a sum over the activities assessed rather than
     * an average over the records carrying them.
     */
    @Test
    public void shouldMeasureActivitiesAsSumsOverActivitiesAssessed() {
        // given (2400 points over 30 activities)
        stubTrend(Map.of(
            "p1#ACTIVITIES", new DataQualityAggregator.PeriodMetric(10, 99.0, 0, 2400.0, 30)));

        // when
        var activities = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 5).trendByEntityType().get(3);

        // then (the record average of the same bucket is ignored)
        assertEquals(80.0, activities.current());
    }

    @Test
    public void shouldReportNoActivityValueWhenNoActivityWasAssessed() {
        // given
        stubTrend(Map.of(
            "p1#ACTIVITIES", new DataQualityAggregator.PeriodMetric(10, 99.0, 0, 0.0, 0)));

        // when
        var activities = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 5).trendByEntityType().get(3);

        // then
        assertNull(activities.current());
    }

    @Test
    public void shouldFallBackToAnEmptySeriesWhenAggregationIsUnavailable() {
        // given (the report degrades rather than failing)
        when(dataQualityAggregator.aggregateMetricByPeriod(any(), any(), any()))
            .thenReturn(Optional.empty());

        // when
        var trend = repositoryAnalyticsService.getQualityTrend(PROFILE, null,
            TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 3);

        // then
        assertEquals(3, trend.series().size());
        trend.series().forEach(point -> assertNull(point.value()));
        assertNull(trend.indicators().current());
    }

    @Test
    public void shouldExportTheTrendWithItsThreePanels() {
        // given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubTrend(Map.of(
            "p0", periodMetric(10, 72.0),
            "p1", periodMetric(10, 84.7)));

        // when
        var rows = exportedRows(repositoryAnalyticsService.exportQualityTrend(
            PROFILE, null, TrendMetric.OVERALL_SCORE, TrendGranularity.WEEKLY, 2, "en"));

        // then
        assertEquals("repositoryAnalytics.qualityTrends", rows.getFirst().getFirst());

        List.of("repositoryAnalytics.currentIndicators", "repositoryAnalytics.metricOverTime",
                "repositoryAnalytics.trendByEntityType")
            .forEach(panel -> assertTrue(rows.stream().anyMatch(row -> row.contains(panel))));

        // Percentages are stored as fractions with a percent format, not as text.
        assertEquals(0.847,
            (double) rowStartingWith(rows, "repositoryAnalytics.header.currentValue").get(1),
            0.0001);
    }

    private void stubTrend(Map<String, DataQualityAggregator.PeriodMetric> values) {
        when(dataQualityAggregator.aggregateMetricByPeriod(any(), any(), any()))
            .thenReturn(Optional.of(values));
    }

    private Map<?, ?> capturedPeriodFilters(int invocation) {
        var captor = ArgumentCaptor.forClass(Map.class);
        verify(dataQualityAggregator, atLeastOnce())
            .aggregateMetricByPeriod(any(), captor.capture(), any());

        return captor.getAllValues().get(invocation);
    }

    private DataQualityAggregator.MetricAggregation capturedMetricAggregation() {
        var captor = ArgumentCaptor.forClass(DataQualityAggregator.MetricAggregation.class);
        verify(dataQualityAggregator, atLeastOnce())
            .aggregateMetricByPeriod(any(), any(), captor.capture());

        return captor.getValue();
    }

    private List<List<Object>> exportedRows(InputStreamResource report) {
        try (var workbook = new XSSFWorkbook(report.getInputStream())) {
            var rows = new ArrayList<List<Object>>();

            workbook.getSheetAt(0).forEach(row -> {
                var values = new ArrayList<Object>();

                row.forEach(cell -> values.add(
                    cell.getCellType() == CellType.NUMERIC
                        ? cell.getNumericCellValue()
                        : cell.getStringCellValue()));

                rows.add(values);
            });

            return rows;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<Object> rowStartingWith(List<List<Object>> rows, String firstCell) {
        return rows.stream()
            .filter(row -> !row.isEmpty() && firstCell.equals(row.getFirst()))
            .findFirst()
            .orElseThrow();
    }

    @Test
    public void shouldExportTheOverviewPanelsToASpreadsheet() {
        // given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubAggregates(1483281, 18426, 30, 8, 1278000, 84.7, 1000, 300);
        stubTopFailedRule("orcidNotResolvable", 4281);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var rows = exportedRows(repositoryAnalyticsService.exportOverview(
                PROFILE, null, LocalDate.of(2026, 7, 18), "en"));

            // then
            assertEquals("repositoryAnalytics.repositoryQualityOverview",
                rows.getFirst().getFirst());

            // The four cards, as numeric cells.
            assertEquals(0.847,
                (double) rowStartingWith(rows, "repositoryAnalytics.header.averageScore").get(1),
                0.0001);
            assertEquals(18426.0,
                rowStartingWith(rows, "repositoryAnalytics.header.openIssues").get(1));
            assertEquals(1483281.0,
                rowStartingWith(rows, "repositoryAnalytics.header.recordsAssessed").get(1));

            // Both panels follow, each behind its own heading.
            assertTrue(rows.stream().anyMatch(
                row -> !row.isEmpty() &&
                    "repositoryAnalytics.qualityByEntityType".equals(row.getFirst())));
            assertTrue(rows.stream().anyMatch(
                row -> !row.isEmpty() &&
                    "repositoryAnalytics.issuesRequiringAttention".equals(row.getFirst())));

            var persons = rowStartingWith(rows, "repositoryAnalytics.entityType.PERSONS");
            assertEquals(3, persons.size());
        }
    }

    @Test
    public void shouldExportThePrevalentIssueTitleOfTheRequestedLanguage() {
        // given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubAggregates(10, 5, 0, 0, 5, 90.0, 10, 0);
        stubTopFailedRule("orcidNotResolvable", 4281);

        try (var configurationLoader = mockConfigurationLoader()) {
            configurationLoader
                .when(() -> DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                    anyString(), anyString(), anyString()))
                .thenReturn(Set.of(multilingualContent("Resolvable ORCID")));

            // when
            var rows = exportedRows(repositoryAnalyticsService.exportOverview(
                PROFILE, null, null, "en"));

            // then (the issue rows carry the title, not the rule key)
            var issueRow = rows.stream()
                .filter(row -> row.size() == 3 && "4281.0".equals(String.valueOf(row.get(2))))
                .findFirst()
                .orElseThrow();

            assertEquals("Resolvable ORCID", issueRow.get(1));
        }
    }

    @Test
    public void shouldExportEveryEntityTypeRowToASpreadsheet() {
        // given
        when(dataQualityAggregator.countRecords(eq("person"), any())).thenReturn(48620L);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            var rows = exportedRows(repositoryAnalyticsService.exportQualityByEntityType(
                PROFILE, null, LocalDate.of(2026, 7, 18), "en"));

            // then (the file has to say what it describes)
            assertEquals("repositoryAnalytics.qualityByEntityType", rows.getFirst().getFirst());
            assertEquals(PROFILE, rowStartingWith(rows, "repositoryAnalytics.profile").get(1));
            assertEquals("2026-07-18",
                rowStartingWith(rows, "repositoryAnalytics.assessmentDate").get(1));

            var persons = rowStartingWith(rows, "repositoryAnalytics.entityType.PERSONS");

            // Counts and percentages are numbers, not text - percentages as Excel fractions.
            assertEquals(48620.0, persons.get(1));
            assertEquals(0.92, (double) persons.get(2), 0.0001);
            assertEquals(0.90, (double) persons.get(3), 0.0001);
            assertEquals(50.0, persons.get(4));
            assertEquals(112.0, persons.get(5));

            // Unsupported rows carry no figures at all.
            var projects = rowStartingWith(rows, "repositoryAnalytics.entityType.PROJECTS");
            assertEquals("-", projects.get(1));
            assertEquals("-", projects.get(2));
        }
    }

    @Test
    public void shouldExportDimensionsToASpreadsheet() {
        // given
        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        stubDimensions(Map.of(
            QualityDimension.ACCURACY,
            new DataQualityAggregator.DimensionAggregates(82.4, 6428, 24190)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var rows = exportedRows(repositoryAnalyticsService.exportQualityByDimension(
                PROFILE, null, null, "en"));

            // then
            assertEquals("repositoryAnalytics.qualityByDimension", rows.getFirst().getFirst());

            // No date means the repository as it stands now.
            assertEquals("repositoryAnalytics.currentState",
                rowStartingWith(rows, "repositoryAnalytics.assessmentDate").get(1));

            var accuracy = rowStartingWith(rows, "repositoryAnalytics.dimension.ACCURACY");
            assertEquals(0.824, (double) accuracy.get(1), 0.0001);
            assertEquals(6428.0, accuracy.get(2));
            assertEquals(24190.0, accuracy.get(3));

            // A dimension nothing was assessed against reports no score.
            var integrity = rowStartingWith(rows, "repositoryAnalytics.dimension.INTEGRITY");
            assertEquals("-", integrity.get(1));
        }
    }

    private void stubDimensions(
        Map<QualityDimension, DataQualityAggregator.DimensionAggregates> aggregates) {
        when(dataQualityAggregator.aggregateDimensions(any())).thenReturn(Optional.of(aggregates));
    }

    @Test
    public void shouldReturnOneRowPerQualityDimension() {
        // given
        stubDimensions(Map.of());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByDimension(PROFILE, null, null);

            // then
            assertEquals(QualityDimension.values().length, result.size());
            assertEquals(List.of(QualityDimension.values()),
                result.stream().map(row -> row.dimension()).toList());
        }
    }

    @Test
    public void shouldCarryFiguresOfEveryAggregatedDimension() {
        // given
        stubDimensions(Map.of(
            QualityDimension.ACCURACY,
            new DataQualityAggregator.DimensionAggregates(82.4, 6428, 24190),
            QualityDimension.CONSISTENCY,
            new DataQualityAggregator.DimensionAggregates(86.0, 4190, 17448)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByDimension(PROFILE, null, null);

            // then
            var accuracy = result.stream()
                .filter(row -> row.dimension() == QualityDimension.ACCURACY)
                .findFirst()
                .orElseThrow();

            assertEquals(82.4, accuracy.averageScore());
            assertEquals(6428, accuracy.openIssues());
            assertEquals(24190, accuracy.affectedRecords());

            var consistency = result.stream()
                .filter(row -> row.dimension() == QualityDimension.CONSISTENCY)
                .findFirst()
                .orElseThrow();

            assertEquals(86.0, consistency.averageScore());
            assertEquals(4190, consistency.openIssues());
        }
    }

    @Test
    public void shouldReturnEmptyFiguresForDimensionsNothingWasAssessedAgainst() {
        // given (a dimension no rule of the profile touches)
        stubDimensions(Map.of(
            QualityDimension.ACCURACY,
            new DataQualityAggregator.DimensionAggregates(82.4, 6428, 24190)));

        try (var ignored = mockConfigurationLoader()) {
            // when
            var integrity = repositoryAnalyticsService.getQualityByDimension(PROFILE, null, null)
                .stream()
                .filter(row -> row.dimension() == QualityDimension.INTEGRITY)
                .findFirst()
                .orElseThrow();

            // then
            assertNull(integrity.averageScore());
            assertEquals(0, integrity.openIssues());
            assertEquals(0, integrity.affectedRecords());
        }
    }

    @Test
    public void shouldReturnEveryDimensionEmptyWhenAggregationIsUnavailable() {
        // given
        when(dataQualityAggregator.aggregateDimensions(any())).thenReturn(Optional.empty());

        try (var ignored = mockConfigurationLoader()) {
            // when
            var result = repositoryAnalyticsService.getQualityByDimension(PROFILE, null, null);

            // then
            assertEquals(QualityDimension.values().length, result.size());
            result.forEach(row -> {
                assertNull(row.averageScore());
                assertEquals(0, row.openIssues());
                assertEquals(0, row.affectedRecords());
            });
        }
    }

    @Test
    public void shouldScopeDimensionsToTheOrganisationUnitSubHierarchyAndRequestedDay() {
        // given
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8));

        stubDimensions(Map.of());

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByDimension(
                PROFILE, 7, LocalDate.of(2026, 7, 18));

            // then
            var captor = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator).aggregateDimensions(captor.capture());

            var query = captor.getValue().toString();
            assertTrue(query.contains("organisation_unit_ids"));
            assertTrue(query.contains("8"));
            assertTrue(query.contains("2026-07-18T23:59:59.999"));

            // Dimensions describe every kind of record, so nothing narrows the entity type.
            assertFalse(query.contains("entity_type"));
        }
    }

    @Test
    public void shouldCountOrganisationUnitsOfTheSubHierarchyByTheirOwnIds() {
        // given
        when(organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(7))
            .thenReturn(List.of(7, 8));

        stubAggregates(50, 120, 30, 8, 45, 92.0, 1000, 300);

        try (var ignored = mockConfigurationLoader()) {
            // when
            repositoryAnalyticsService.getQualityByEntityType(PROFILE, 7, null);

            // then
            var countQuery = ArgumentCaptor.forClass(Query.class);
            verify(dataQualityAggregator)
                .countRecords(eq("organisation_unit"), countQuery.capture());

            var query = countQuery.getValue().toString();
            assertTrue(query.contains("databaseId"));
            assertTrue(query.contains("8"));
        }
    }
}
