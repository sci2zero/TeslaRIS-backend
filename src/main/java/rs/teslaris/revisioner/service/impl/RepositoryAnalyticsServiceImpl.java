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
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import rs.teslaris.revisioner.dto.EntityTypeTrendDTO;
import rs.teslaris.revisioner.dto.IssueStatisticsDTO;
import rs.teslaris.revisioner.dto.PrevalentIssueDTO;
import rs.teslaris.revisioner.dto.PublicationCandidateAnalysisDTO;
import rs.teslaris.revisioner.dto.QualityTrendDTO;
import rs.teslaris.revisioner.dto.RepositoryOverviewDTO;
import rs.teslaris.revisioner.dto.SeverityBreakdownDTO;
import rs.teslaris.revisioner.dto.TrendIndicatorsDTO;
import rs.teslaris.revisioner.dto.TrendPointDTO;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.service.interfaces.RepositoryAnalyticsService;
import rs.teslaris.revisioner.util.dataquality.DataQualityAggregator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;
import rs.teslaris.revisioner.util.dataquality.TrendGranularity;
import rs.teslaris.revisioner.util.dataquality.TrendMetric;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryAnalyticsServiceImpl implements RepositoryAnalyticsService {

    private static final String DOCUMENT_TARGET = "Document";

    private static final String ACTIVITY_TARGET = "Activity";

    private static final String PERSON_TARGET = "Person";

    private static final String ORGANISATION_UNIT_TARGET = "OrganisationUnit";

    private static final String BLOCKING_RULE_KEYS_FIELD = "blocking_rule_keys";

    private static final String PERSON_INDEX = "person";

    private static final String ACTIVITIES_COUNT_FIELD = "activities_count";

    private static final String ORGANISATION_UNIT_INDEX = "organisation_unit";

    private static final String EMPTY_VALUE = "-";

    private static final int TOP_RECURRING_CONSTRAINT_COUNT = 5;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private static final List<String> ACTIVITY_PARENT_TARGETS =
        List.of(DOCUMENT_TARGET, PERSON_TARGET);

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

    @Override
    @Transactional(readOnly = true)
    public PublicationCandidateAnalysisDTO getPublicationCandidateAnalysis(
        String profileName, Integer organisationUnitId, @Nullable LocalDate assessmentDate) {

        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);
        var repositoryWide = assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
            MatchAllQuery.of(matchAll -> matchAll)._toQuery());

        var everything = dataQualityAggregator
            .aggregateAssessments(repositoryWide, Set.of())
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        var blocking = dataQualityAggregator
            .aggregateBlocking(repositoryWide)
            .orElseGet(DataQualityAggregator.BlockingAggregates::empty);

        return new PublicationCandidateAnalysisDTO(
            everything.publicationCandidates(),
            everything.affectedRecords() - everything.publicationCandidates(),
            percentage(everything.publicationCandidates(), everything.affectedRecords()),
            blocking.distinctBlockingConstraints(),
            blocking.blockingIssues(),
            getQualityByEntityType(profileName, organisationUnitId, assessmentDate),
            mostCommonBlockingConstraints(profileName, scopeOrganisationUnitIds, assessmentDate)
        );
    }

    /**
     * The blocking rule that fails most often for each entity type. Only rules the profile marks as
     * blocking can keep a record from being a publication candidate, so the report is restricted to
     * those, both the rule keys it considers and the index field it aggregates.
     */
    private List<PrevalentIssueDTO> mostCommonBlockingConstraints(
        String profileName, List<Integer> scopeOrganisationUnitIds,
        @Nullable LocalDate assessmentDate) {

        return List.of(
            prevalentBlockingIssue(RepositoryEntityType.PERSONS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.PERSON.name())),
                PERSON_TARGET),
            prevalentBlockingIssue(RepositoryEntityType.ORGANISATION_UNITS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.ORGANISATION_UNIT.name())),
                ORGANISATION_UNIT_TARGET),
            prevalentBlockingIssue(RepositoryEntityType.OUTPUTS, profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("target", DOCUMENT_TARGET)),
                DOCUMENT_TARGET),
            prevalentActivityIssue(profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    stringTermsQuery("target", ACTIVITY_PARENT_TARGETS)),
                blockingRuleKeys(profileName, ACTIVITY_TARGET)),
            // TODO: projects carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.PROJECTS),
            // TODO: fundings carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.FUNDINGS)
        );
    }

    private PrevalentIssueDTO prevalentBlockingIssue(RepositoryEntityType entityType,
                                                     String profileName, Query query,
                                                     String target) {
        var topRule = dataQualityAggregator.topFailedRule(query,
            blockingRuleKeys(profileName, target), BLOCKING_RULE_KEYS_FIELD);

        return describeTopRule(entityType, profileName, topRule);
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
            prevalentActivityIssue(profileName,
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    stringTermsQuery("target", ACTIVITY_PARENT_TARGETS)),
                ruleKeys(profileName, ACTIVITY_TARGET)),
            // TODO: projects carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.PROJECTS),
            // TODO: fundings carry no quality assessments yet.
            PrevalentIssueDTO.none(RepositoryEntityType.FUNDINGS)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public IssueStatisticsDTO getIssueStatistics(String profileName, Integer organisationUnitId,
                                                 @Nullable LocalDate assessmentDate) {
        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);

        var persons = issueBreakdown(profileName, scopeOrganisationUnitIds, assessmentDate,
            termQuery("entity_type", EntityType.PERSON.name()));
        var organisationUnits = issueBreakdown(profileName, scopeOrganisationUnitIds,
            assessmentDate, termQuery("entity_type", EntityType.ORGANISATION_UNIT.name()));
        var outputs = issueBreakdown(profileName, scopeOrganisationUnitIds, assessmentDate,
            termQuery("target", DOCUMENT_TARGET));

        var rows = List.of(
            severityRow(RepositoryEntityType.PERSONS, persons),
            severityRow(RepositoryEntityType.ORGANISATION_UNITS, organisationUnits),
            severityRow(RepositoryEntityType.OUTPUTS, outputs),
            activityRow(List.of(persons, outputs)),
            // TODO: projects carry no quality assessments yet.
            SeverityBreakdownDTO.unsupported(RepositoryEntityType.PROJECTS),
            // TODO: fundings carry no quality assessments yet.
            SeverityBreakdownDTO.unsupported(RepositoryEntityType.FUNDINGS)
        );

        var errorIssues = rows.stream().mapToLong(SeverityBreakdownDTO::errorIssues).sum();
        var warningIssues = rows.stream().mapToLong(SeverityBreakdownDTO::warningIssues).sum();
        var infoIssues = rows.stream().mapToLong(SeverityBreakdownDTO::infoIssues).sum();

        return new IssueStatisticsDTO(
            errorIssues + warningIssues + infoIssues,
            errorIssues,
            warningIssues,
            infoIssues,
            rows,
            topRecurringConstraints(profileName, scopeOrganisationUnitIds, assessmentDate)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public QualityTrendDTO getQualityTrend(String profileName, Integer organisationUnitId,
                                           TrendMetric metric, TrendGranularity granularity,
                                           @Nullable Integer points) {
        var scopeOrganisationUnitIds = organisationUnitScope(organisationUnitId);
        var periodEnds = periodEnds(granularity, granularity.resolvePoints(points));
        var aggregation = metricAggregation(metric);

        var baseQuery = trendBaseQuery(profileName, scopeOrganisationUnitIds);

        var series = seriesOf(metric, granularity, periodEnds,
            dataQualityAggregator.aggregateMetricByPeriod(baseQuery,
                    periodFilters(periodEnds), aggregation)
                .orElseGet(Map::of));

        return new QualityTrendDTO(
            metric,
            granularity,
            series,
            indicatorsOf(series),
            trendByEntityType(metric, baseQuery, periodEnds, aggregation)
        );
    }

    /**
     * The instants the series is measured at, oldest first. Each is the last millisecond of its
     * period, so the newest point matches what every other tab shows for "current state".
     */
    private List<LocalDate> periodEnds(TrendGranularity granularity, int points) {
        var today = LocalDate.now(ZoneOffset.UTC);
        var ends = new ArrayList<LocalDate>();

        for (var period = points - 1; period >= 0; period--) {
            ends.add(switch (granularity) {
                case DAILY -> today.minusDays(period);
                case WEEKLY -> today.minusWeeks(period);
                case MONTHLY -> today.minusMonths(period);
            });
        }

        return ends;
    }

    private Query trendBaseQuery(String profileName, List<Integer> scopeOrganisationUnitIds) {
        var clauses = new ArrayList<Query>();
        clauses.add(termQuery("profile_name", profileName));

        if (!scopeOrganisationUnitIds.isEmpty()) {
            clauses.add(intTermsQuery("organisation_unit_ids", scopeOrganisationUnitIds));
        }

        return BoolQuery.of(b -> b.must(clauses))._toQuery();
    }

    /**
     * The two range clauses that select the assessment current at that instant, one named filter
     * per point of the series.
     */
    private Map<String, Query> periodFilters(List<LocalDate> periodEnds) {
        var filters = new LinkedHashMap<String, Query>();

        for (var period = 0; period < periodEnds.size(); period++) {
            filters.put(periodKey(period), pointInTimeQuery(periodEnds.get(period)));
        }

        return filters;
    }

    private Query pointInTimeQuery(LocalDate periodEnd) {
        var instant = INSTANT_FORMAT.format(
            LocalDateTime.ofInstant(periodEnd.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC));

        return BoolQuery.of(b -> b
            .must(rangeQuery("assessment_date", instant, true))
            .must(rangeQuery("valid_to", instant, false))
        )._toQuery();
    }

    private String periodKey(int period) {
        return "p" + period;
    }

    private DataQualityAggregator.MetricAggregation metricAggregation(TrendMetric metric) {
        if (metric.isRate()) {
            return new DataQualityAggregator.MetricAggregation(null, "publication_candidate",
                "activity_publication_candidates_count");
        }

        if (metric.isDimension()) {
            return new DataQualityAggregator.MetricAggregation(
                metric.dimension().name().toLowerCase(Locale.ROOT) + "_score", null,
                "activity_dimension_score_sums." + metric.dimension().name());
        }

        return switch (metric) {
            case FAIR_COMPLIANCE -> new DataQualityAggregator.MetricAggregation(
                "quality_score_fair", null, "activity_fair_score_sum");
            default -> new DataQualityAggregator.MetricAggregation(
                "quality_score", null, "activity_score_sum");
        };
    }

    private List<TrendPointDTO> seriesOf(TrendMetric metric, TrendGranularity granularity,
                                         List<LocalDate> periodEnds,
                                         Map<String, DataQualityAggregator.PeriodMetric> values) {
        var series = new ArrayList<TrendPointDTO>();

        for (var period = 0; period < periodEnds.size(); period++) {
            var periodEnd = periodEnds.get(period);
            var value = values.getOrDefault(periodKey(period),
                DataQualityAggregator.PeriodMetric.empty());

            series.add(new TrendPointDTO(
                periodLabel(granularity, periodEnd),
                periodEnd,
                recordMetricValue(metric, value),
                value.recordsAssessed()
            ));
        }

        return series;
    }

    private String periodLabel(TrendGranularity granularity, LocalDate periodEnd) {
        return switch (granularity) {
            case DAILY -> periodEnd.format(DateTimeFormatter.ISO_LOCAL_DATE);
            // Built from the ISO week fields rather than a pattern, because 'w' in a pattern
            // resolves against the default locale's week rules.
            case WEEKLY -> String.format("%d-W%02d",
                periodEnd.get(IsoFields.WEEK_BASED_YEAR),
                periodEnd.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MONTHLY -> periodEnd.format(MONTH_FORMAT);
        };
    }

    /**
     * Records answer a score with an average and a candidate rate with a share of the bucket.
     */
    @Nullable
    private Double recordMetricValue(TrendMetric metric,
                                     DataQualityAggregator.PeriodMetric value) {
        if (!metric.isRate()) {
            return value.average();
        }

        return percentage(value.matchingRecords(), value.recordsAssessed());
    }

    /**
     * Activities are not records, so their figures are sums over the activities assessed rather
     * than an average over the records carrying them.
     */
    @Nullable
    private Double activityMetricValue(TrendMetric metric,
                                       DataQualityAggregator.PeriodMetric value) {
        if (value.activityCount() == 0) {
            return null;
        }

        var perActivity = value.activitySum() / value.activityCount();

        return metric.isRate() ? perActivity * 100.0 : perActivity;
    }

    private TrendIndicatorsDTO indicatorsOf(List<TrendPointDTO> series) {
        var values = series.stream()
            .map(TrendPointDTO::value)
            .filter(Objects::nonNull)
            .toList();

        if (values.isEmpty()) {
            return TrendIndicatorsDTO.empty();
        }

        var current = series.getLast().value();
        var previous = series.size() > 1 ? series.get(series.size() - 2).value() : null;

        return new TrendIndicatorsDTO(
            current,
            previous,
            change(current, previous),
            values.stream().mapToDouble(Double::doubleValue).max().orElseThrow(),
            values.stream().mapToDouble(Double::doubleValue).min().orElseThrow()
        );
    }

    @Nullable
    private Double change(@Nullable Double current, @Nullable Double previous) {
        return Objects.isNull(current) || Objects.isNull(previous) ? null : current - previous;
    }

    /**
     * The newest two points of the series, per entity type. Both periods and all four supported
     * scopes are named filters of one aggregation, so the whole panel costs a single request.
     */
    private List<EntityTypeTrendDTO> trendByEntityType(
        TrendMetric metric, Query baseQuery, List<LocalDate> periodEnds,
        DataQualityAggregator.MetricAggregation aggregation) {

        var scopes = new LinkedHashMap<RepositoryEntityType, Query>();
        scopes.put(RepositoryEntityType.PERSONS,
            termQuery("entity_type", EntityType.PERSON.name()));
        scopes.put(RepositoryEntityType.ORGANISATION_UNITS,
            termQuery("entity_type", EntityType.ORGANISATION_UNIT.name()));
        scopes.put(RepositoryEntityType.OUTPUTS, termQuery("target", DOCUMENT_TARGET));
        scopes.put(RepositoryEntityType.ACTIVITIES,
            stringTermsQuery("target", ACTIVITY_PARENT_TARGETS));

        var latestPeriods = periodEnds.size() > 1
            ? periodEnds.subList(periodEnds.size() - 2, periodEnds.size())
            : periodEnds;

        var filters = new LinkedHashMap<String, Query>();

        for (var period = 0; period < latestPeriods.size(); period++) {
            var pointInTime = pointInTimeQuery(latestPeriods.get(period));

            for (var scope : scopes.entrySet()) {
                filters.put(entityTypeKey(period, scope.getKey()),
                    BoolQuery.of(b -> b.must(pointInTime).must(scope.getValue()))._toQuery());
            }
        }

        var values = dataQualityAggregator
            .aggregateMetricByPeriod(baseQuery, filters, aggregation)
            .orElseGet(Map::of);

        var previousPeriod = latestPeriods.size() > 1 ? 0 : -1;
        var currentPeriod = latestPeriods.size() - 1;

        var rows = new ArrayList<EntityTypeTrendDTO>();

        scopes.keySet().forEach(entityType -> {
            var current = entityTypeValue(metric, entityType, values, currentPeriod);
            var previous = entityTypeValue(metric, entityType, values, previousPeriod);

            rows.add(new EntityTypeTrendDTO(entityType, current, previous,
                change(current, previous), true));
        });

        // TODO: projects and fundings carry no quality assessments yet.
        rows.add(EntityTypeTrendDTO.unsupported(RepositoryEntityType.PROJECTS));
        rows.add(EntityTypeTrendDTO.unsupported(RepositoryEntityType.FUNDINGS));

        return rows;
    }

    @Nullable
    private Double entityTypeValue(TrendMetric metric, RepositoryEntityType entityType,
                                   Map<String, DataQualityAggregator.PeriodMetric> values,
                                   int period) {
        if (period < 0) {
            return null;
        }

        var value = values.getOrDefault(entityTypeKey(period, entityType),
            DataQualityAggregator.PeriodMetric.empty());

        return RepositoryEntityType.ACTIVITIES.equals(entityType)
            ? activityMetricValue(metric, value)
            : recordMetricValue(metric, value);
    }

    private String entityTypeKey(int period, RepositoryEntityType entityType) {
        return periodKey(period) + "#" + entityType.name();
    }

    private DataQualityAggregator.IssueBreakdown issueBreakdown(
        String profileName, List<Integer> scopeOrganisationUnitIds,
        @Nullable LocalDate assessmentDate, Query scope) {

        return dataQualityAggregator
            .aggregateIssueBreakdown(
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate, scope))
            .orElseGet(DataQualityAggregator.IssueBreakdown::empty);
    }

    private SeverityBreakdownDTO severityRow(RepositoryEntityType entityType,
                                             DataQualityAggregator.IssueBreakdown breakdown) {
        return new SeverityBreakdownDTO(
            entityType,
            Math.max(0, breakdown.errorIssues() - breakdown.activityErrorIssues()),
            Math.max(0, breakdown.warningIssues() - breakdown.activityWarningIssues()),
            Math.max(0, breakdown.infoIssues() - breakdown.activityInfoIssues()),
            true
        );
    }

    private SeverityBreakdownDTO activityRow(
        List<DataQualityAggregator.IssueBreakdown> breakdowns) {

        return new SeverityBreakdownDTO(
            RepositoryEntityType.ACTIVITIES,
            breakdowns.stream().mapToLong(
                DataQualityAggregator.IssueBreakdown::activityErrorIssues).sum(),
            breakdowns.stream().mapToLong(
                DataQualityAggregator.IssueBreakdown::activityWarningIssues).sum(),
            breakdowns.stream().mapToLong(
                DataQualityAggregator.IssueBreakdown::activityInfoIssues).sum(),
            true
        );
    }

    /**
     * The rules failing on the most records across the whole repository, regardless of the family
     * they belong to.
     */
    private List<PrevalentIssueDTO> topRecurringConstraints(
        String profileName, List<Integer> scopeOrganisationUnitIds,
        @Nullable LocalDate assessmentDate) {

        return dataQualityAggregator.topFailedRules(
                assessmentQuery(profileName, scopeOrganisationUnitIds, assessmentDate,
                    MatchAllQuery.of(matchAll -> matchAll)._toQuery()),
                ruleKeys(profileName, null),
                TOP_RECURRING_CONSTRAINT_COUNT)
            .stream()
            .map(rule -> describeTopRule(null, profileName, Optional.of(rule)))
            .toList();
    }

    /**
     * Activities are not records of their own - one document or person raises an issue per
     * offending contribution or involvement - so this row reports how many activities a rule
     * affected, not how many records it failed on, which is all a distinct-key aggregation can say.
     */
    private PrevalentIssueDTO prevalentActivityIssue(String profileName, Query query,
                                                     Set<String> ruleKeys) {
        return describeTopRule(RepositoryEntityType.ACTIVITIES, profileName,
            dataQualityAggregator.topRuleByActivityOccurrences(query, ruleKeys));
    }

    private PrevalentIssueDTO prevalentIssue(RepositoryEntityType entityType, String profileName,
                                             Query query, String target) {
        return describeTopRule(entityType, profileName,
            dataQualityAggregator.topFailedRule(query, ruleKeys(profileName, target)));
    }

    private PrevalentIssueDTO describeTopRule(
        RepositoryEntityType entityType, String profileName,
        Optional<DataQualityAggregator.TopFailedRule> topRule) {

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

        // Person assessments now carry activity issues of their own, raised by the person's
        // involvements, so this pass answers both the Persons row and part of the Activities one.
        var persons = dataQualityAggregator
            .aggregateAssessments(
                assessmentQuery(
                    profileName, scopeOrganisationUnitIds, assessmentDate,
                    termQuery("entity_type", EntityType.PERSON.name())
                ),
                activityRuleKeys)
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
            constructQualityByEntityTypeActivitiesRow(
                documents.linkedActivities() + personActivities(scopeOrganisationUnitIds),
                outputs, persons),
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
    public InputStreamResource exportPublicationCandidateAnalysis(
        String profileName, Integer organisationUnitId, @Nullable LocalDate assessmentDate,
        String language) {
        var analysis = getPublicationCandidateAnalysis(profileName, organisationUnitId,
            assessmentDate);

        var rows = reportContext("repositoryAnalytics.publicationCandidateAnalysis", profileName,
            assessmentDate, language);

        rows.add(List.of(label("repositoryAnalytics.header.publicationCandidates", language),
            analysis.publicationCandidates()));
        rows.add(List.of(label("repositoryAnalytics.header.notPublicationCandidates", language),
            analysis.notPublicationCandidates()));
        rows.add(List.of(label("repositoryAnalytics.header.candidateRate", language),
            percentage(analysis.candidateRate())));
        rows.add(List.of(label("repositoryAnalytics.header.blockingConstraints", language),
            analysis.blockingConstraints()));
        rows.add(List.of(label("repositoryAnalytics.header.blockingIssues", language),
            analysis.blockingIssues()));
        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.candidateRateByEntityType", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.candidateRate", language)
        ));

        analysis.candidateRateByEntityType().forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + row.entityType().name(), language),
                percentage(row.publicationCandidatePercentage())
            )));

        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.mostCommonBlockingConstraints", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.constraint", language),
            label("repositoryAnalytics.header.occurrences", language)
        ));

        analysis.mostCommonBlockingConstraints().forEach(issue ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + issue.entityType().name(), language),
                issueTitle(issue, language),
                Objects.isNull(issue.ruleKey()) ? EMPTY_VALUE : issue.occurrences()
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportQualityTrend(String profileName, Integer organisationUnitId,
                                                  TrendMetric metric,
                                                  TrendGranularity granularity,
                                                  @Nullable Integer points, String language) {
        var trend = getQualityTrend(profileName, organisationUnitId, metric, granularity, points);

        var rows = reportContext("repositoryAnalytics.qualityTrends", profileName, null, language);

        rows.add(List.of(label("repositoryAnalytics.header.metric", language),
            label("repositoryAnalytics.metric." + metric.name(), language)));
        rows.add(List.of(label("repositoryAnalytics.header.granularity", language),
            label("repositoryAnalytics.granularity." + granularity.name(), language)));
        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.currentIndicators", language)));
        rows.add(List.of(label("repositoryAnalytics.header.currentValue", language),
            percentage(trend.indicators().current())));
        rows.add(List.of(label("repositoryAnalytics.header.previousValue", language),
            percentage(trend.indicators().previous())));
        rows.add(List.of(label("repositoryAnalytics.header.change", language),
            percentage(trend.indicators().change())));
        rows.add(List.of(label("repositoryAnalytics.header.bestValue", language),
            percentage(trend.indicators().best())));
        rows.add(List.of(label("repositoryAnalytics.header.lowestValue", language),
            percentage(trend.indicators().lowest())));
        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.metricOverTime", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.period", language),
            label("repositoryAnalytics.header.value", language),
            label("repositoryAnalytics.header.recordsAssessed", language)
        ));

        trend.series().forEach(point ->
            rows.add(List.of(
                point.label(),
                percentage(point.value()),
                point.recordsAssessed()
            )));

        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.trendByEntityType", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.currentValue", language),
            label("repositoryAnalytics.header.previousValue", language),
            label("repositoryAnalytics.header.change", language)
        ));

        trend.trendByEntityType().forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + row.entityType().name(), language),
                row.supported() ? percentage(row.current()) : EMPTY_VALUE,
                row.supported() ? percentage(row.previous()) : EMPTY_VALUE,
                row.supported() ? percentage(row.change()) : EMPTY_VALUE
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportIssueStatistics(String profileName, Integer organisationUnitId,
                                                     @Nullable LocalDate assessmentDate,
                                                     String language) {
        var statistics = getIssueStatistics(profileName, organisationUnitId, assessmentDate);

        var rows = reportContext("repositoryAnalytics.issueStatistics", profileName,
            assessmentDate, language);

        rows.add(List.of(label("repositoryAnalytics.header.openIssues", language),
            statistics.openIssues()));
        rows.add(List.of(label("repositoryAnalytics.header.errorIssues", language),
            statistics.errorIssues()));
        rows.add(List.of(label("repositoryAnalytics.header.warningIssues", language),
            statistics.warningIssues()));
        rows.add(List.of(label("repositoryAnalytics.header.infoIssues", language),
            statistics.infoIssues()));
        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.issuesBySeverityAndEntityType", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.entityType", language),
            label("repositoryAnalytics.header.errorIssues", language),
            label("repositoryAnalytics.header.warningIssues", language),
            label("repositoryAnalytics.header.infoIssues", language)
        ));

        statistics.issuesBySeverityAndEntityType().forEach(row ->
            rows.add(List.of(
                label("repositoryAnalytics.entityType." + row.entityType().name(), language),
                count(row.supported(), row.errorIssues()),
                count(row.supported(), row.warningIssues()),
                count(row.supported(), row.infoIssues())
            )));

        rows.add(List.of());

        rows.add(List.of(label("repositoryAnalytics.topRecurringConstraints", language)));
        rows.add(List.of(
            label("repositoryAnalytics.header.constraint", language),
            label("repositoryAnalytics.header.occurrences", language)
        ));

        statistics.topRecurringConstraints().forEach(constraint ->
            rows.add(List.of(
                issueTitle(constraint, language),
                constraint.occurrences()
            )));

        return TableExportHelper.createTypedXLSXFile(rows);
    }

    @Override
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
            aggregates.openIssues() - aggregates.activityIssues(),
            aggregates.averageScore(),
            percentage(aggregates.publicationCandidates(), aggregates.affectedRecords()),
            true
        );
    }

    /**
     * An activity is not a record, so its score and candidacy cannot be read off the assessment
     * document that carries it.
     */
    private EntityTypeQualityDTO constructQualityByEntityTypeActivitiesRow(
        long records, DataQualityAggregator.AssessmentAggregates outputs,
        DataQualityAggregator.AssessmentAggregates persons) {

        var assessedActivities = outputs.activitiesCount() + persons.activitiesCount();
        var scoreSum = outputs.activityScoreSum() + persons.activityScoreSum();
        var candidates =
            outputs.activityPublicationCandidates() + persons.activityPublicationCandidates();

        return new EntityTypeQualityDTO(
            RepositoryEntityType.ACTIVITIES,
            records,
            assessedActivities,
            outputs.activityIssues() + persons.activityIssues(),
            assessedActivities > 0 ? scoreSum / assessedActivities : null,
            percentage(candidates, assessedActivities),
            true
        );
    }

    /**
     * Activities recorded as a person's involvements. They are counted on the person record rather
     * than on an output, so they are summed from the person index and added to the activities the
     * outputs carry.
     */
    private long personActivities(List<Integer> scopeOrganisationUnitIds) {
        return dataQualityAggregator.sumField(PERSON_INDEX,
            scopedQuery("employment_institutions_id", scopeOrganisationUnitIds),
            ACTIVITIES_COUNT_FIELD);
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
            clauses.add(intTermsQuery("organisation_unit_ids", scopeOrganisationUnitIds));
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
            : intTermsQuery(field, scopeOrganisationUnitIds);
    }

    private Query intTermsQuery(String field, List<Integer> values) {
        return TermsQuery.of(terms -> terms
            .field(field)
            .terms(termValues -> termValues.value(values.stream().map(FieldValue::of).toList()))
        )._toQuery();
    }

    private Query stringTermsQuery(String field, List<String> values) {
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
    /**
     * @return the keys of the family's rules the profile marks as blocking, across every configured
     * version of the profile
     */
    private Set<String> blockingRuleKeys(String profileName, String target) {
        var keys = new HashSet<String>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion().stream()
            .filter(profileAndVersion -> profileAndVersion.a.equalsIgnoreCase(profileName))
            .forEach(profileAndVersion ->
                DataQualityAssessmentConfigurationLoader
                    .getRulesForTarget(profileAndVersion.a, profileAndVersion.b, List.of(target))
                    .stream()
                    .filter(rule -> rule.getValue().blocking())
                    .forEach(rule -> keys.add(rule.getKey())));

        return keys;
    }

    private List<Integer> organisationUnitScope(Integer organisationUnitId) {
        return Objects.isNull(organisationUnitId)
            ? List.of()
            : organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);
    }
}
