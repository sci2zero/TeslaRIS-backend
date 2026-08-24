package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.service.interfaces.RepositoryAnalyticsService;
import rs.teslaris.revisioner.util.dataquality.DataQualityAggregator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryAnalyticsServiceImpl implements RepositoryAnalyticsService {

    private static final String DOCUMENT_TARGET = "Document";

    private static final String ACTIVITY_TARGET = "Activity";

    private static final String PERSON_INDEX = "person";

    private static final String ORGANISATION_UNIT_INDEX = "organisation_unit";

    private static final DateTimeFormatter INSTANT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final DataQualityAggregator dataQualityAggregator;

    private final OrganisationUnitService organisationUnitService;


    @Override
    @Transactional(readOnly = true)
    public List<EntityTypeQualityDTO> getQualityByEntityType(String profileName,
                                                             Integer organisationUnitId,
                                                             @Nullable LocalDate assessmentDate) {
        var activityRuleKeys = activityRuleKeys(profileName);

        var scopeOrganisationUnitIds = Objects.isNull(organisationUnitId)
            ? List.<Integer>of()
            : organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var persons = dataQualityAggregator
            .aggregateAssessments(
                assessmentQuery(
                    profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.PERSON.name())
                ),
                Set.of())
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        var organisationUnits = dataQualityAggregator
            .aggregateAssessments(
                assessmentQuery(
                    profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.ORGANISATION_UNIT.name())
                ),
                Set.of())
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        // Activities are contributions recorded on outputs rather than records of their own, so the
        // one pass over output assessments answers both rows.
        var outputs = dataQualityAggregator
            .aggregateAssessments(
                assessmentQuery(
                    profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("target", DOCUMENT_TARGET)
                ),
                activityRuleKeys)
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        var documents = dataQualityAggregator
            .aggregateLinkedDocuments(
                scopedQuery("organisation_unit_ids", scopeOrganisationUnitIds))
            .orElseGet(DataQualityAggregator.LinkedDocumentAggregates::empty);

        return List.of(
            constructQualityByEntityTypeRowData(RepositoryEntityType.PERSONS,
                dataQualityAggregator.countRecords(PERSON_INDEX,
                    scopedQuery("employment_institutions_id", scopeOrganisationUnitIds)),
                persons),
            constructQualityByEntityTypeRowData(RepositoryEntityType.ORGANISATION_UNITS,
                dataQualityAggregator.countRecords(ORGANISATION_UNIT_INDEX,
                    scopedQuery("databaseId", scopeOrganisationUnitIds)),
                organisationUnits),
            constructQualityByEntityTypeRowData(RepositoryEntityType.OUTPUTS,
                documents.linkedRecords(), outputs),
            constructQualityByEntityTypeActivitiesRow(documents.linkedActivities(), outputs),
            // TODO: projects carry no quality assessments yet.
            EntityTypeQualityDTO.unsupported(RepositoryEntityType.PROJECTS),
            // TODO: fundings carry no quality assessments yet.
            EntityTypeQualityDTO.unsupported(RepositoryEntityType.FUNDINGS)
        );
    }

    private EntityTypeQualityDTO constructQualityByEntityTypeRowData(
        RepositoryEntityType entityType,
        long records,
        DataQualityAggregator.AssessmentAggregates aggregates) {
        return new EntityTypeQualityDTO(
            entityType,
            records,
            aggregates.affectedRecords(),
            aggregates.openIssues(),
            aggregates.averageScore(),
            percentage(aggregates.publicationCandidates(), aggregates.affectedRecords()),
            true
        );
    }

    /**
     * Activities are never scored and can never be publication candidates.
     * The Activity target is reported but excluded from scoring  so both figures stay empty.
     */
    private EntityTypeQualityDTO constructQualityByEntityTypeActivitiesRow(long records,
                                                                           DataQualityAggregator.AssessmentAggregates outputs) {
        return new EntityTypeQualityDTO(
            RepositoryEntityType.ACTIVITIES,
            records,
            outputs.activitiesCount(),
            outputs.activityIssues(),
            null,
            null,
            true
        );
    }

    @Nullable
    private Double percentage(long part, long total) {
        return total > 0 ? (part * 100.0) / total : null;
    }

    /**
     * Assessments are immutable and their validity intervals are half-open and contiguous per
     * entity and profile, so a single instant matches exactly one assessment per record. Resolving
     * the requested day to its last millisecond is what makes the newest assessment of that day win
     * when a record was edited more than once.
     */
    private Query assessmentQuery(String profileName, List<Integer> scopeOrganisationUnitIds,
                                  @Nullable LocalDate assessmentDate, Query scope) {
        var clauses = new ArrayList<Query>();
        clauses.add(scope);
        clauses.add(termQuery("profile_name", profileName));

        if (!scopeOrganisationUnitIds.isEmpty()) {
            clauses.add(termsQuery("organisation_unit_ids", scopeOrganisationUnitIds));
        }

        if (Objects.isNull(assessmentDate)) {
            clauses.add(TermQuery.of(t -> t.field("is_latest").value(true))._toQuery());
        } else {
            var instant = INSTANT_FORMAT.format(
                LocalDateTime.ofInstant(
                    assessmentDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC),
                    ZoneOffset.UTC));

            clauses.add(rangeQuery("assessment_date", instant, true));
            clauses.add(rangeQuery("valid_to", instant, false));
        }

        return BoolQuery.of(b -> b.must(clauses))._toQuery();
    }

    private Query rangeQuery(String field, String instant, boolean lowerBound) {
        return RangeQuery.of(range -> {
            range.field(field);

            return lowerBound
                ? range.lte(JsonData.of(instant))
                : range.gt(JsonData.of(instant));
        })._toQuery();
    }

    /**
     * @return a query narrowed to the requested organisation units, or a match-all when the report
     * covers the whole repository
     */
    private Query scopedQuery(String field, List<Integer> scopeOrganisationUnitIds) {
        return scopeOrganisationUnitIds.isEmpty()
            ? MatchAllQuery.of(matchAll -> matchAll)._toQuery()
            : termsQuery(field, scopeOrganisationUnitIds);
    }

    private Query termsQuery(String field, List<Integer> values) {
        return TermsQuery.of(terms -> terms
            .field(field)
            .terms(termValues -> termValues.value(values.stream().map(FieldValue::of).toList()))
        )._toQuery();
    }

    private Query termQuery(String field, String value) {
        return TermQuery.of(t -> t.field(field).value(value))._toQuery();
    }

    private Set<String> activityRuleKeys(String profileName) {
        var keys = new HashSet<String>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion().stream()
            .filter(profileAndVersion -> profileAndVersion.a.equalsIgnoreCase(profileName))
            .forEach(profileAndVersion -> keys.addAll(
                DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    profileAndVersion.a, profileAndVersion.b, ACTIVITY_TARGET, null, null)));

        return keys;
    }
}
