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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.impl.TableExportHelper;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.revisioner.dto.DimensionQualityDTO;
import rs.teslaris.revisioner.dto.EntityTypeQualityDTO;
import rs.teslaris.revisioner.dto.PrevalentIssueDTO;
import rs.teslaris.revisioner.dto.RepositoryOverviewDTO;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
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

    private static final String PERSON_TARGET = "Person";

    private static final String ORGANISATION_UNIT_TARGET = "OrganisationUnit";

    private static final String PERSON_INDEX = "person";

    private static final String ORGANISATION_UNIT_INDEX = "organisation_unit";

    private static final String EMPTY_VALUE = "-";

    private static final DateTimeFormatter INSTANT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final DataQualityAggregator dataQualityAggregator;

    private final OrganisationUnitService organisationUnitService;

    private final MessageSource messageSource;


    @Override
    @Transactional(readOnly = true)
    public RepositoryOverviewDTO getOverview(String profileName, Integer organisationUnitId,
                                             @Nullable LocalDate assessmentDate) {
        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);

        // The cards describe the repository as a whole, so nothing narrows the kind of record.
        var everything = dataQualityAggregator
            .aggregateAssessments(
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    MatchAllQuery.of(matchAll -> matchAll)._toQuery()),
                Set.of())
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        return new RepositoryOverviewDTO(
            everything.averageScore(),
            percentage(everything.publicationCandidates(), everything.affectedRecords()),
            everything.openIssues(),
            everything.affectedRecords(),
            getQualityByEntityType(profileName, organisationUnitId, assessmentDate),
            issuesRequiringAttention(profileName, scopeOrganisationUnitIds, assessmentDate)
        );
    }

    /**
     * The rule that fails most often for each entity type. One aggregation per row,
     * each bounded by the rule keys of that row's target family.
     */
    private List<PrevalentIssueDTO> issuesRequiringAttention(
        String profileName, List<Integer> scopeOrganisationUnitIds,
        @Nullable LocalDate assessmentDate) {

        return List.of(
            prevalentIssue(RepositoryEntityType.PERSONS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.PERSON.name())),
                PERSON_TARGET),
            prevalentIssue(RepositoryEntityType.ORGANISATION_UNITS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.ORGANISATION_UNIT.name())),
                ORGANISATION_UNIT_TARGET),
            prevalentIssue(RepositoryEntityType.OUTPUTS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("target", DOCUMENT_TARGET)),
                DOCUMENT_TARGET),
            prevalentIssue(RepositoryEntityType.ACTIVITIES, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("target", DOCUMENT_TARGET)),
                ACTIVITY_TARGET),
            // TODO: projects carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.PROJECTS),
            // TODO: fundings carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.FUNDINGS)
        );
    }

    private PrevalentIssueDTO prevalentIssue(RepositoryEntityType entityType, String profileName,
                                             Query query, String target) {
        var topRule = dataQualityAggregator.topFailedRule(query, ruleKeys(profileName, target));

        if (topRule.isEmpty()) {
            return PrevalentIssueDTO.none(entityType);
        }

        var version = DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(profileName);

        return new PrevalentIssueDTO(
            entityType,
            topRule.get().ruleKey(),
            MultilingualContentConverter.getMultilingualContentDTO(
                DataQualityAssessmentConfigurationLoader.getDataQualityTitle(profileName, version,
                    topRule.get().ruleKey())),
            topRule.get().occurrences()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityTypeQualityDTO> getQualityByEntityType(String profileName,
                                                             Integer organisationUnitId,
                                                             @Nullable LocalDate assessmentDate) {
        var activityRuleKeys = activityRuleKeys(profileName);

        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);

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

    @Override
    @Transactional(readOnly = true)
    public List<DimensionQualityDTO> getQualityByDimension(String profileName,
                                                           Integer organisationUnitId,
                                                           @Nullable LocalDate assessmentDate) {
        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);

        // Dimensions describe the repository as a whole, so every assessed record counts regardless
        // of what kind of record it is.
        var aggregates = dataQualityAggregator
            .aggregateDimensions(assessmentQuery(profileName, scopeOrganisationUnitIds,
                assessmentDate, MatchAllQuery.of(matchAll -> matchAll)._toQuery()))
            .orElseGet(Map::of);

        return Arrays.stream(QualityDimension.values())
            .map(dimension -> {
                var dimensionAggregates = aggregates.get(dimension);

                return Objects.isNull(dimensionAggregates)
                    ? new DimensionQualityDTO(dimension, null, 0, 0)
                    : new DimensionQualityDTO(
                    dimension,
                    dimensionAggregates.averageScore(),
                    dimensionAggregates.openIssues(),
                    dimensionAggregates.affectedRecords());
            })
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportOverview(String profileName, Integer organisationUnitId,
                                              @Nullable LocalDate assessmentDate,
                                              String language) {
        var overview = getOverview(profileName, organisationUnitId, assessmentDate);

        var rows = reportContext("repositoryAnalytics.repositoryQualityOverview", profileName,
            assessmentDate, language);

        // The three panels of the tab, one after another, each behind its own heading.
        rows.add(List.of(label("repositoryAnalytics.header.averageScore", language),
            percentage(overview.averageScore())));
        rows.add(List.of(label("repositoryAnalytics.header.publicationCandidates", language),
            percentage(overview.publicationCandidatePercentage())));
        rows.add(List.of(label("repositoryAnalytics.header.openIssues", language),
            overview.openIssues()));
        rows.add(List.of(label("repositoryAnalytics.header.recordsAssessed", language),
            overview.recordsAssessed()));
        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.qualityByEntityType", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.averageScore", language),
            label("repositoryAnalytics.header.openIssues", language)
        ));

        overview.qualityByEntityType().forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + row.entityType().name(), language),
                percentage(row.averageScore()),
                count(row.supported(), row.openIssues())
            )));

        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.issuesRequiringAttention", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.constraint", language),
            label("repositoryAnalytics.header.occurrences", language)
        ));

        overview.issuesRequiringAttention().forEach(issue ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + issue.entityType().name(), language),
                issueTitle(issue, language),
                Objects.isNull(issue.ruleKey()) ? EMPTY_VALUE : issue.occurrences()
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    /**
     * @return the localised title of the prevalent issue, falling back to its key when the profile
     * carries no title for it, or the placeholder when the entity type has no issues at all
     */
    private Object issueTitle(PrevalentIssueDTO issue, String language) {
        if (Objects.isNull(issue.ruleKey())) {
            return EMPTY_VALUE;
        }

        return issue.title().stream()
            .filter(title -> title.getLanguageTag().equalsIgnoreCase(language))
            .findFirst()
            .map(MultilingualContentDTO::getContent)
            .orElseGet(() -> issue.title().isEmpty()
                ? issue.ruleKey()
                : issue.title().getFirst().getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportQualityByEntityType(String profileName,
                                                         Integer organisationUnitId,
                                                         @Nullable LocalDate assessmentDate,
                                                         String language) {
        var rows = reportContext("repositoryAnalytics.qualityByEntityType", profileName,
            assessmentDate, language);

        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.records", language),
            label("repositoryAnalytics.header.averageScore", language),
            label("repositoryAnalytics.header.publicationCandidates", language),
            label("repositoryAnalytics.header.affectedRecords", language),
            label("repositoryAnalytics.header.openIssues", language)
        ));

        getQualityByEntityType(profileName, organisationUnitId, assessmentDate).forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + row.entityType().name(), language),
                count(row.supported(), row.records()),
                percentage(row.averageScore()),
                percentage(row.publicationCandidatePercentage()),
                count(row.supported(), row.affectedRecords()),
                count(row.supported(), row.openIssues())
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportQualityByDimension(String profileName,
                                                        Integer organisationUnitId,
                                                        @Nullable LocalDate assessmentDate,
                                                        String language) {
        var rows = reportContext("repositoryAnalytics.qualityByDimension", profileName,
            assessmentDate, language);

        rows.add(List.of(
            label("repositoryAnalytics.header.dimension", language),
            label("repositoryAnalytics.header.averageScore", language),
            label("repositoryAnalytics.header.openIssues", language),
            label("repositoryAnalytics.header.affectedRecords", language)
        ));

        getQualityByDimension(profileName, organisationUnitId, assessmentDate).forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.dimension." + row.dimension().name(), language),
                percentage(row.averageScore()),
                row.openIssues(),
                row.affectedRecords()
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    /**
     * The exported file has to say what it describes - without the profile and the day it was taken
     * for, the numbers cannot be told apart from another export.
     */
    private List<List<Object>> reportContext(String titleKey, String profileName,
                                             @Nullable LocalDate assessmentDate, String language) {
        var rows = new ArrayList<List<Object>>();

        rows.add(List.of(label(titleKey, language)));
        rows.add(List.of(label("repositoryAnalytics.profile", language), profileName));
        rows.add(List.of(
            label("repositoryAnalytics.assessmentDate", language),
            Objects.isNull(assessmentDate)
                ? label("repositoryAnalytics.currentState", language)
                : assessmentDate.toString()));
        rows.add(List.of());

        return rows;
    }

    private String label(String key, String language) {
        try {
            return messageSource.getMessage(key, null, Locale.forLanguageTag(language));
        } catch (Exception e) {
            return key;
        }
    }

    /**
     * @return a numeric percentage cell, or the placeholder when there is nothing to report - a
     * dash keeps an empty row readable, and a zero would read as a real score
     */
    private Object percentage(@Nullable Double value) {
        return Objects.isNull(value)
            ? EMPTY_VALUE
            : new TableExportHelper.PercentageValue(value);
    }

    private Object count(boolean supported, long value) {
        return supported ? value : EMPTY_VALUE;
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
        return ruleKeys(profileName, ACTIVITY_TARGET);
    }

    /**
     * Documents of several profile versions can coexist under one profile name, so a family's keys
     * are the union of every configured version's.
     */
    private Set<String> ruleKeys(String profileName, String target) {
        var keys = new HashSet<String>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion().stream()
            .filter(profileAndVersion -> profileAndVersion.a.equalsIgnoreCase(profileName))
            .forEach(profileAndVersion -> keys.addAll(
                DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    profileAndVersion.a, profileAndVersion.b, target, null, null)));

        return keys;
    }

    /**
     * An organisation unit stands for everything below it, so the whole sub-hierarchy scopes a
     * report - both the assessments it aggregates and the totals they are shown against, otherwise
     * a unit would be measured against the whole repository.
     */
    private List<Integer> organisationUnitScope(Integer organisationUnitId) {
        return Objects.isNull(organisationUnitId)
            ? List.of()
            : organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
    }
}
