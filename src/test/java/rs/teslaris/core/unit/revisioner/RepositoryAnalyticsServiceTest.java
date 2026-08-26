package rs.teslaris.core.unit.revisioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        when(dataQualityAggregator.aggregateAssessments(any(), any()))
            .thenReturn(Optional.of(new DataQualityAggregator.AssessmentAggregates(
                affectedRecords, openIssues, activitiesCount, activityIssues,
                publicationCandidates, averageScore)));
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

            // The rules of each family are what the aggregation is restricted to.
            verify(dataQualityAggregator, times(4)).topFailedRule(any(), any());
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
            assertEquals(120, persons.openIssues());
            assertEquals(92.0, persons.averageScore());

            // 45 of the 50 assessed records are publication candidates.
            assertEquals(90.0, persons.publicationCandidatePercentage());
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

            // Activities live on outputs, so their totals are sums of the activity counters.
            var activities = result.get(3);
            assertEquals(86204, activities.records());
            assertEquals(30, activities.affectedRecords());
            assertEquals(8, activities.openIssues());

            // The Activity target is reported but never scored.
            assertNull(activities.averageScore());
            assertNull(activities.publicationCandidatePercentage());
            assertTrue(activities.supported());

            verify(dataQualityAggregator).aggregateLinkedDocuments(any());
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
            assertEquals(120.0, persons.get(5));

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
