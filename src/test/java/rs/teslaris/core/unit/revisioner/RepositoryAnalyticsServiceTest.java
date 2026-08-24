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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.functional.Pair;
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
