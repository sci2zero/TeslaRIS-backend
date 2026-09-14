package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.converter.DataQualityAssessmentConverter;
import rs.teslaris.revisioner.converter.DataQualityProfileConverter;
import rs.teslaris.revisioner.converter.IssueConverter;
import rs.teslaris.revisioner.converter.IssueDetailsConverter;
import rs.teslaris.revisioner.dto.ConstraintSummaryDTO;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
import rs.teslaris.revisioner.dto.DataQualityIssuePageDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileSummaryDTO;
import rs.teslaris.revisioner.dto.ProfileRelatedQualityDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.dto.RelatedQualityDTO;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.repository.DataQualityAssessmentRepository;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.dataquality.DataQualityAggregator;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.IssueCursor;
import rs.teslaris.revisioner.util.dataquality.PointInTimeQueries;
import rs.teslaris.revisioner.util.dataquality.RelatedEntityType;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityServiceImpl implements DataQualityService {

    private static final String DOCUMENT_TARGET = "Document";

    private static final String PERSON_TARGET = "Person";

    private static final String ACTIVITY_TARGET = "Activity";

    private static final String PERSON_INDEX = "person";

    private static final String ORGANISATION_UNIT_INDEX = "organisation_unit";

    private static final String ACTIVITIES_COUNT_FIELD = "activities_count";

    private static final int ISSUE_SCAN_BATCH_SIZE = 500;

    private static final List<String> DOCUMENT_PERSON_ROLE_FIELDS = List.of(
        "author_ids", "editor_ids", "reviewer_ids", "board_member_ids", "advisor_ids",
        "presenter_ids", "translator_ids", "assistant_staff_ids", "arguer_ids", "owner_ids",
        "associated_editor_ids", "invited_editor_ids"
    );

    private static final Comparator<PendingIssue> ISSUE_ORDER =
        Comparator.comparing(PendingIssue::cursor, IssueCursor.WITHIN_RECORD);

    private static final int DEFAULT_ISSUE_PAGE_SIZE = 50;

    private static final int MAX_ISSUE_PAGE_SIZE = 100;

    private static final String ISSUE_INDEX_NAME = "data_quality_assessment";

    private final EntityRevisionRepository entityRevisionRepository;

    private final DataQualityAssessmentRepository dataQualityAssessmentRepository;

    private final LanguageTagService languageTagService;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final SearchService<DataQualityAssessmentIndex> searchService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final DataQualityAggregator dataQualityAggregator;

    private final OrganisationUnitService organisationUnitService;

    private final PersonIndexRepository personIndexRepository;


    @Override
    @Transactional(readOnly = true)
    public List<QualityReportResponseDTO> getQualityReportForEntity(String entityType,
                                                                    Integer entityId) {
        var entityRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId);

        if (entityRevision.isEmpty()) {
            return List.of();
        }

        var qualityReport = new ArrayList<QualityReportResponseDTO>();

        entityRevision.get().getAssessments().forEach(assessment -> {
            List<Pair<IssueSeverity, List<MultilingualContentDTO>>> assessmentReport =
                new ArrayList<>();

            assessment.getIssues().forEach(issue -> {
                var remarks = DataQualityAssessmentConfigurationLoader.getDataQualityRemark(
                    assessment.getProfileName(),
                    assessment.getProfileVersion(),
                    issue.getKey(),
                    issue.getParameters().toArray()
                );

                var multilingualContents = remarks.stream()
                    .map(r -> new MultilingualContentDTO(
                        r.getLanguage().getId(),
                        r.getLanguage().getLanguageTag(),
                        r.getContent(),
                        r.getPriority()
                    ))
                    .toList();

                assessmentReport.add(new Pair<>(issue.getSeverity(), multilingualContents));
            });

            qualityReport.add(
                new QualityReportResponseDTO(
                    assessment.getProfileName() + " (" + assessment.getProfileVersion() + ")",
                    assessment.getQualityScore(),
                    assessment.getInfoFailedRules() +
                        assessment.getWarningFailedRules() +
                        assessment.getErrorFailedRules(),
                    LocalDate.ofInstant(assessment.getStartedAt(), ZoneId.systemDefault()),
                    assessment.getPublicationCandidate(),
                    assessmentReport
                )
            );
        });

        return qualityReport;
    }

    @Transactional(readOnly = true)
    public List<DataQualityAssessmentDTO> findLatestAssessmentsForEntity(String entityType,
                                                                         Integer entityId) {
        var entityRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId)
            .orElseThrow(() -> new NotFoundException(
                "No data quality assessment found for " + entityType + " with ID " + entityId +
                    "."));

        return entityRevision.getAssessments().stream()
            .sorted(Comparator.comparing(DataQualityAssessment::getProfileName))
            .map(DataQualityAssessmentConverter::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataQualityAssessmentDTO> findAssessmentsForEntityVersion(String entityType,
                                                                          Integer entityId,
                                                                          Integer majorVersion,
                                                                          Integer minorVersion) {
        var entityRevision = entityRevisionRepository
            .findFirstByEntityTypeAndEntityIdAndMajorVersionAndMinorVersionOrderByRevisionTimestampDesc(
                entityType, entityId, majorVersion, minorVersion)
            .orElseThrow(() -> new NotFoundException(
                String.format("Revision %d.%d of %s with ID %d does not exist.",
                    majorVersion, minorVersion, entityType, entityId)));

        return entityRevision.getAssessments().stream()
            .sorted(Comparator.comparing(DataQualityAssessment::getProfileName))
            .map(DataQualityAssessmentConverter::toDTO)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileRelatedQualityDTO> getRelatedQualityForEntity(String entityType,
                                                                     Integer entityId) {
        var latestRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId);

        return latestRevision.map(entityRevision -> entityRevision.getAssessments().stream()
            .sorted(Comparator.comparing(DataQualityAssessment::getProfileName))
            .map(assessment -> new ProfileRelatedQualityDTO(
                assessment.getProfileName(),
                assessment.getProfileVersion(),
                assessment.getFinishedAt(),
                relatedQuality(entityType, entityId, assessment.getProfileName())))
            .toList()).orElseGet(List::of);
    }

    private List<RelatedQualityDTO> relatedQuality(String entityType, Integer entityId,
                                                   String profileName) {
        var isPerson = EntityType.PERSON.name().equals(entityType);
        var isOrganisationUnit = EntityType.ORGANISATION_UNIT.name().equals(entityType);

        if (!isPerson && !isOrganisationUnit) {
            return Arrays.stream(RelatedEntityType.values())
                .map(RelatedQualityDTO::unsupported)
                .toList();
        }

        // An organisation unit answers for everything below it, and not every record carries its
        // ancestors: a thesis is indexed under its own institution only, and person, event and
        // organisation unit assessments under their direct one. Expanding the sub-hierarchy here
        // makes the scope hold whatever the indexer stored.
        var scopeIds = organisationUnitScope(isOrganisationUnit, entityId);

        var activityRuleKeys = expandRuleKeys(profileName, ACTIVITY_TARGET, null, null, null);

        var assessments = dataQualityAggregator
            .aggregateAssessments(
                relatedAssessmentsQuery(isPerson, entityId, scopeIds, profileName),
                activityRuleKeys)
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        // Involvements are activities recorded on the person rather than on an output, so the
        // person assessments in scope have to be counted alongside the output ones.
        var personAssessments = dataQualityAggregator
            .aggregateAssessments(
                relatedPersonAssessmentsQuery(isPerson, entityId, scopeIds, profileName),
                activityRuleKeys)
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        var documents = dataQualityAggregator
            .aggregateLinkedDocuments(linkedDocumentsQuery(isPerson, entityId, scopeIds))
            .orElseGet(DataQualityAggregator.LinkedDocumentAggregates::empty);

        var personActivities = dataQualityAggregator.sumField(PERSON_INDEX,
            linkedPersonsQuery(isPerson, entityId, scopeIds), ACTIVITIES_COUNT_FIELD);

        var assessedActivities =
            assessments.activitiesCount() + personAssessments.activitiesCount();

        return List.of(
            relatedPersons(isPerson, scopeIds, personAssessments),
            relatedOrganisationUnits(isPerson, entityId, scopeIds, profileName),
            new RelatedQualityDTO(
                RelatedEntityType.OUTPUTS,
                documents.linkedRecords(),
                assessments.affectedRecords(),
                assessments.openIssues() - assessments.activityIssueOccurrences(),
                assessments.averageScore(),
                true
            ),
            // Activities live on other records, so every figure here is a sum of activity
            // counters. The score is the sum of the per-activity scores over the number of
            // activities assessed, which weights each activity equally no matter how many the
            // record carrying it holds.
            new RelatedQualityDTO(
                RelatedEntityType.ACTIVITIES,
                documents.linkedActivities() + personActivities,
                assessedActivities,
                assessments.activityIssues() + personAssessments.activityIssues(),
                averageActivityScore(assessments, personAssessments, assessedActivities),
                true
            ),
            // TODO: projects have no quality assessments yet.
            RelatedQualityDTO.unsupported(RelatedEntityType.PROJECTS),
            // TODO: fundings have no quality assessments yet.
            RelatedQualityDTO.unsupported(RelatedEntityType.FUNDINGS)
        );
    }

    // A person has no related persons in the index; a unit has everyone employed below it.
    private RelatedQualityDTO relatedPersons(
        boolean isPerson, List<Integer> scopeIds,
        DataQualityAggregator.AssessmentAggregates personAssessments) {

        if (isPerson) {
            return RelatedQualityDTO.unsupported(RelatedEntityType.PERSONS);
        }

        return new RelatedQualityDTO(
            RelatedEntityType.PERSONS,
            dataQualityAggregator.countRecords(PERSON_INDEX,
                termsQuery("employment_institutions_id", scopeIds)),
            personAssessments.affectedRecords(),
            personAssessments.openIssues() - personAssessments.activityIssueOccurrences(),
            personAssessments.averageScore(),
            true
        );
    }

    // A person's units are where they are employed; a unit's are itself and everything below it.
    private RelatedQualityDTO relatedOrganisationUnits(boolean isPerson, Integer entityId,
                                                       List<Integer> scopeIds,
                                                       String profileName) {
        var unitIds = isPerson
            ? personIndexRepository.findByDatabaseId(entityId)
            .map(PersonIndex::getEmploymentInstitutionsId)
            .orElseGet(List::of)
            : scopeIds;

        if (unitIds.isEmpty()) {
            return new RelatedQualityDTO(RelatedEntityType.ORGANISATION_UNITS, 0, 0, 0, null,
                true);
        }

        var unitAssessments = dataQualityAggregator
            .aggregateAssessments(
                entityAssessmentsQuery(EntityType.ORGANISATION_UNIT.name(), unitIds, profileName),
                Set.of())
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        return new RelatedQualityDTO(
            RelatedEntityType.ORGANISATION_UNITS,
            dataQualityAggregator.countRecords(ORGANISATION_UNIT_INDEX,
                termsQuery("databaseId", unitIds)),
            unitAssessments.affectedRecords(),
            unitAssessments.openIssues(),
            unitAssessments.averageScore(),
            true
        );
    }

    private Query entityAssessmentsQuery(String entityType, List<Integer> unitIds,
                                         String profileName) {
        return BoolQuery.of(b -> b
            .must(m -> m.term(t -> t.field("entity_type").value(entityType)))
            .must(m -> m.term(t -> t.field("is_latest").value(true)))
            .must(m -> m.term(t -> t.field("profile_name").value(profileName)))
            .must(termsQuery("organisation_unit_ids", unitIds))
        )._toQuery();
    }

    private Query relatedAssessmentsQuery(boolean isPerson, Integer entityId,
                                          List<Integer> scopeIds, String profileName) {
        return BoolQuery.of(b -> b
            .must(m -> m.term(t -> t.field("target").value(DOCUMENT_TARGET)))
            .must(m -> m.term(t -> t.field("is_latest").value(true)))
            .must(m -> m.term(t -> t.field("profile_name").value(profileName)))
            .must(isPerson
                ? TermQuery.of(t -> t.field("related_person_ids").value(entityId))._toQuery()
                : termsQuery("organisation_unit_ids", scopeIds))
        )._toQuery();
    }

    @Nullable
    private Double averageActivityScore(
        DataQualityAggregator.AssessmentAggregates assessments,
        DataQualityAggregator.AssessmentAggregates personAssessments,
        long assessedActivities) {
        if (assessedActivities == 0) {
            return null;
        }

        return (assessments.activityScoreSum() + personAssessments.activityScoreSum()) /
            assessedActivities;
    }

    /**
     * The person assessments whose involvements count towards this entity: the person itself, or
     * every person employed anywhere in the organisation unit's sub-hierarchy.
     */
    private Query relatedPersonAssessmentsQuery(boolean isPerson, Integer entityId,
                                                List<Integer> scopeIds, String profileName) {
        return BoolQuery.of(b -> b
            .must(m -> m.term(t -> t.field("entity_type").value(EntityType.PERSON.name())))
            .must(m -> m.term(t -> t.field("is_latest").value(true)))
            .must(m -> m.term(t -> t.field("profile_name").value(profileName)))
            .must(isPerson
                ? TermQuery.of(t -> t.field("entity_id").value(entityId))._toQuery()
                : termsQuery("organisation_unit_ids", scopeIds))
        )._toQuery();
    }

    private Query linkedPersonsQuery(boolean isPerson, Integer entityId, List<Integer> scopeIds) {
        return isPerson
            ? TermQuery.of(t -> t.field("databaseId").value(entityId))._toQuery()
            : termsQuery("employment_institutions_id", scopeIds);
    }

    private Query linkedDocumentsQuery(boolean isPerson, Integer entityId,
                                       List<Integer> scopeIds) {
        if (!isPerson) {
            return termsQuery("organisation_unit_ids", scopeIds);
        }

        var roleClauses = DOCUMENT_PERSON_ROLE_FIELDS.stream()
            .map(field -> TermQuery.of(t -> t.field(field).value(entityId))._toQuery())
            .toList();

        return BoolQuery.of(b -> b.should(roleClauses).minimumShouldMatch("1"))._toQuery();
    }

    @Override
    @Transactional(readOnly = true)
    public DataQualityIssuePageDTO findIssuesForEntity(String entityType, Integer entityId,
                                                       String profileName, String target,
                                                       QualityDimension dimension,
                                                       IssueSeverity severity,
                                                       String constraintKey,
                                                       @Nullable LocalDate assessmentDate,
                                                       @Nullable String cursor,
                                                       @Nullable Integer size) {
        var isOrganisationUnit = EntityType.ORGANISATION_UNIT.name().equals(entityType);

        var query = buildIssueQuery(
            entityScopeClause(entityType, entityId,
                organisationUnitScope(isOrganisationUnit, entityId)),
            profileName, issueTargets(target), assessmentDate);

        return issuePage(query, profileName, target, dimension, severity, constraintKey, cursor,
            size);
    }

    @Override
    @Transactional(readOnly = true)
    public DataQualityIssuePageDTO findRepositoryIssues(@Nullable Integer organisationUnitId,
                                                        String profileName, String target,
                                                        QualityDimension dimension,
                                                        IssueSeverity severity,
                                                        String constraintKey,
                                                        @Nullable LocalDate assessmentDate,
                                                        @Nullable String cursor,
                                                        @Nullable Integer size) {
        // No unit means the whole repository; a unit means everything in its sub-hierarchy.
        var scopeIds = Objects.isNull(organisationUnitId)
            ? List.<Integer>of()
            : organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(organisationUnitId);

        var query = buildIssueQuery(
            scopeIds.isEmpty() ? null : termsQuery("organisation_unit_ids", scopeIds),
            profileName, issueTargets(target), assessmentDate);

        return issuePage(query, profileName, target, dimension, severity, constraintKey, cursor,
            size);
    }

    private DataQualityIssuePageDTO issuePage(Query query, String profileName, String target,
                                              QualityDimension dimension, IssueSeverity severity,
                                              String constraintKey, @Nullable String cursor,
                                              @Nullable Integer size) {
        var window = collectIssueWindow(query, target, dimension, severity, constraintKey,
            Objects.isNull(cursor) ? null : IssueCursor.decode(cursor), pageSize(size));

        var totalIssues = countIssues(query, profileName, target, dimension, severity,
            constraintKey, window.issues().size());

        return new DataQualityIssuePageDTO(window.issues(), totalIssues, window.nextCursor());
    }

    private int pageSize(@Nullable Integer size) {
        return Math.max(1, Math.min(MAX_ISSUE_PAGE_SIZE,
            Objects.requireNonNullElse(size, DEFAULT_ISSUE_PAGE_SIZE)));
    }

    private List<String> issueTargets(String target) {
        if (Objects.isNull(target)) {
            return List.of();
        }

        return ACTIVITY_TARGET.equals(target)
            ? List.of(DOCUMENT_TARGET, PERSON_TARGET)
            : List.of(target);
    }

    private IssueWindow collectIssueWindow(Query query, String target, QualityDimension dimension,
                                           IssueSeverity severity, String constraintKey,
                                           @Nullable IssueCursor cursor, int pageSize) {
        var window = new ArrayList<PendingIssue>();
        var block = new ArrayList<PendingIssue>();
        var applicableKeys = new HashMap<String, Set<String>>();

        // One row beyond the page tells whether there is a next page without a second scan.
        var wanted = pageSize + 1;

        var emittedAtWatermark = new HashSet<String>();
        Integer watermark = Objects.isNull(cursor) ? null : cursor.entityId();
        Integer blockEntityId = null;

        while (true) {
            var batch = searchService.runQueryWithoutTotal(
                withWatermark(query, watermark),
                PageRequest.of(0, ISSUE_SCAN_BATCH_SIZE, Sort.by(Sort.Direction.ASC, "entity_id")),
                DataQualityAssessmentIndex.class,
                ISSUE_INDEX_NAME
            ).getContent();

            if (batch.isEmpty()) {
                break;
            }

            var progressed = false;

            for (var assessment : batch) {
                // The watermark is inclusive so that a batch boundary cannot cut a group of
                // assessments sharing one entity id in half; whatever was already emitted at that
                // id is skipped here instead.
                if (!emittedAtWatermark.add(assessment.getId())) {
                    continue;
                }

                progressed = true;

                if (!Objects.equals(blockEntityId, assessment.getEntityId())) {
                    flushBlock(block, window, cursor, wanted);
                    blockEntityId = assessment.getEntityId();
                }

                if (!Objects.equals(watermark, assessment.getEntityId())) {
                    watermark = assessment.getEntityId();
                    emittedAtWatermark.clear();
                    emittedAtWatermark.add(assessment.getId());
                }

                expandIssues(assessment, target, dimension, severity, constraintKey,
                    applicableKeys, block);
            }

            if (window.size() >= wanted) {
                return finishWindow(window, pageSize);
            }

            if (batch.size() < ISSUE_SCAN_BATCH_SIZE) {
                break;
            }

            if (!progressed) {
                // A single entity id filled an entire batch; step past it rather than rescanning it
                // forever.
                log.warn("More than {} assessments share entity id {}, skipping the remainder.",
                    ISSUE_SCAN_BATCH_SIZE, watermark);

                watermark = Objects.requireNonNullElse(watermark, 0) + 1;
                emittedAtWatermark.clear();
            }
        }

        flushBlock(block, window, cursor, wanted);

        return finishWindow(window, pageSize);
    }

    private void flushBlock(List<PendingIssue> block, List<PendingIssue> window,
                            @Nullable IssueCursor cursor, int wanted) {
        if (block.isEmpty()) {
            return;
        }

        block.sort(ISSUE_ORDER);

        for (var issue : block) {
            if (window.size() >= wanted) {
                break;
            }

            if (!isAtOrBeforeCursor(issue, cursor)) {
                window.add(issue);
            }
        }

        block.clear();
    }

    // Only the record the cursor points into can hold rows already served.
    private boolean isAtOrBeforeCursor(PendingIssue issue, @Nullable IssueCursor cursor) {
        return Objects.nonNull(cursor) &&
            Objects.equals(issue.assessment().getEntityId(), cursor.entityId()) &&
            IssueCursor.WITHIN_RECORD.compare(issue.cursor(), cursor) <= 0;
    }

    // Rows are rendered only here, since each render resolves language tags through the repository.
    private IssueWindow finishWindow(List<PendingIssue> window, int pageSize) {
        var hasMore = window.size() > pageSize;
        var page = window.subList(0, Math.min(pageSize, window.size()));

        var issues = page.stream()
            .map(issue -> IssueConverter.toDTO(issue.assessment(), issue.ruleKey()))
            .toList();

        return new IssueWindow(issues, hasMore ? page.getLast().cursor().encode() : null);
    }

    private void expandIssues(DataQualityAssessmentIndex assessment, String target,
                              QualityDimension dimension, IssueSeverity severity,
                              String constraintKey, Map<String, Set<String>> applicableKeys,
                              List<PendingIssue> collector) {
        var keys = applicableKeys.computeIfAbsent(
            assessment.getProfileName() + "#" + assessment.getProfileVersion(),
            ignored -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                assessment.getProfileName(), assessment.getProfileVersion(), target, dimension,
                severity));

        Objects.requireNonNullElse(assessment.getFailedRuleKeys(), List.<String>of()).stream()
            .filter(keys::contains)
            .filter(ruleKey -> Objects.isNull(constraintKey) || constraintKey.equals(ruleKey))
            .forEach(ruleKey -> collector.add(pendingIssue(assessment, ruleKey)));
    }

    /**
     * Everything the row order depends on, and nothing else - the severity comes from the profile
     * by a map lookup rather than from a rendered DTO.
     */
    private PendingIssue pendingIssue(DataQualityAssessmentIndex assessment, String ruleKey) {
        var remark = DataQualityAssessmentConfigurationLoader.getIssue(
            assessment.getProfileName(), assessment.getProfileVersion(), ruleKey);

        return new PendingIssue(assessment, ruleKey, remark.severity(),
            assessment.getEntityType());
    }

    private Query withWatermark(Query query, Integer watermark) {
        if (Objects.isNull(watermark)) {
            return query;
        }

        return BoolQuery.of(b -> b
            .must(query)
            .must(m -> m.range(r -> r.field("entity_id").gte(JsonData.of(watermark))))
        )._toQuery();
    }

    private long countIssues(Query query, String profileName, String target,
                             QualityDimension dimension, IssueSeverity severity,
                             String constraintKey, long fallback) {
        return dataQualityAggregator
            .countIssues(query,
                expandRuleKeys(profileName, target, dimension, severity, constraintKey))
            .orElse(fallback);
    }

    /**
     * @return the organisation units a report for this entity covers - the unit itself and its
     * sub-hierarchy - or just the entity itself when it is not an organisation unit and no
     * hierarchy applies
     */
    private List<Integer> organisationUnitScope(boolean isOrganisationUnit, Integer entityId) {
        return isOrganisationUnit
            ? organisationUnitService.getOrganisationUnitIdsFromSubHierarchy(entityId)
            : List.of(entityId);
    }

    private Query termsQuery(String field, List<Integer> values) {
        return TermsQuery.of(terms -> terms
            .field(field)
            .terms(termValues -> termValues.value(values.stream().map(FieldValue::of).toList()))
        )._toQuery();
    }

    private Set<String> expandRuleKeys(String profileName, String target,
                                       QualityDimension dimension, IssueSeverity severity,
                                       String constraintKey) {
        var keys = new HashSet<String>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion().stream()
            .filter(profileAndVersion -> profileAndVersion.a.equals(profileName))
            .forEach(profileAndVersion -> keys.addAll(
                DataQualityAssessmentConfigurationLoader.listRuleKeys(
                    profileAndVersion.a, profileAndVersion.b, target, dimension, severity)));

        if (Objects.nonNull(constraintKey)) {
            keys.retainAll(Set.of(constraintKey));
        }

        return keys;
    }

    // The entity's own assessments plus those of the records related to it.
    private Query entityScopeClause(String entityType, Integer entityId, List<Integer> scopeIds) {
        var relatedClause = EntityType.PERSON.name().equals(entityType)
            ? TermQuery.of(tq -> tq.field("related_person_ids").value(entityId))._toQuery()
            : termsQuery("organisation_unit_ids", scopeIds);

        return BoolQuery.of(b -> b
            .should(own -> own.bool(ownEntity -> ownEntity
                .must(m -> m.term(tq -> tq.field("entity_type").value(entityType)))
                .must(m -> m.term(tq -> tq.field("entity_id").value(entityId)))))
            .should(relatedClause)
            .minimumShouldMatch("1")
        )._toQuery();
    }

    private Query buildIssueQuery(@Nullable Query scope, String profileName,
                                  List<String> targets, @Nullable LocalDate assessmentDate) {
        var clauses = new ArrayList<Query>();

        if (Objects.nonNull(scope)) {
            clauses.add(scope);
        }

        clauses.add(PointInTimeQueries.assessmentsValidOn(assessmentDate));
        clauses.add(TermQuery.of(tq -> tq.field("profile_name").value(profileName))._toQuery());

        if (Objects.nonNull(targets) && !targets.isEmpty()) {
            clauses.add(TermsQuery.of(tq -> tq
                .field("target")
                .terms(values -> values.value(targets.stream().map(FieldValue::of).toList()))
            )._toQuery());
        }

        return BoolQuery.of(b -> b.must(clauses))._toQuery();
    }

    @Override
    @Transactional(readOnly = true)
    public DataQualityIssueDetailsDTO findIssueDetails(Integer assessmentId, String ruleKey) {
        var assessment = dataQualityAssessmentRepository
            .findWithRevisionById(assessmentId)
            .orElseThrow(() -> new NotFoundException(
                "Assessment with ID " + assessmentId + " does not exist."));

        // A rule evaluated per contributor or per title fails once per offending value, while the
        // index keeps only the distinct key - so every occurrence of the key belongs to this issue.
        var occurrences = Objects.requireNonNullElse(assessment.getIssues(),
                List.<ConstraintEvaluationResult>of())
            .stream()
            .filter(issue -> ruleKey.equals(issue.getKey()))
            .toList();

        if (occurrences.isEmpty()) {
            throw new NotFoundException(
                String.format("Assessment %d records no failure of rule '%s'.", assessmentId,
                    ruleKey));
        }

        return IssueDetailsConverter.toDTO(assessment, ruleKey, occurrences);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConstraintSummaryDTO> listProfileConstraints(String profileName, String target) {
        var version = DataQualityAssessmentConfigurationLoader.getLatestProfileVersion(profileName);

        // The rules of a target family are what the issues table can be filtered by, and the family
        // is known here - the picker does not need the rest of the profile to work it out. A null
        // target means every rule of the profile.
        return DataQualityAssessmentConfigurationLoader
            .listRuleKeys(profileName, version, target, null, null)
            .stream()
            .sorted()
            .map(ruleKey -> new ConstraintSummaryDTO(
                ruleKey,
                MultilingualContentConverter.getMultilingualContentDTO(
                    DataQualityAssessmentConfigurationLoader.getDataQualityTitle(
                        profileName, version, ruleKey))))
            .toList();
    }

    @Override
    public List<DataQualityProfileSummaryDTO> listDataQualityProfileNames() {
        // Answered from the loaded configuration alone - no rules are converted and no language tag
        // is resolved, which is what makes the full listing expensive.
        return DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion()
            .stream()
            .map(profileAndVersion -> new DataQualityProfileSummaryDTO(
                profileAndVersion.a, profileAndVersion.b))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataQualityProfileDTO> listAllDataQualityProfiles() {
        var allProfiles = new ArrayList<DataQualityProfileDTO>();

        DataQualityAssessmentConfigurationLoader.listAvailableProfilesWithVersion()
            .forEach(profileAndVersion -> {
                var profile =
                    DataQualityAssessmentConfigurationLoader.getProfile(profileAndVersion.a,
                        profileAndVersion.b);

                allProfiles.add(DataQualityProfileConverter.toDTO(profileAndVersion.a, profile,
                    languageTagService));
            });

        return allProfiles;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reassessLatestRevision(String entityType, Integer entityId,
                                          String profileName) {
        var latestRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId);

        if (latestRevision.isEmpty()) {
            return false;
        }

        var revision = latestRevision.get();

        // Only the profile being reassessed is dropped; assessments of the other profiles describe
        // the same revision and stay valid.
        var supersededAssessments = revision.getAssessments().stream()
            .filter(assessment -> Objects.isNull(profileName) ||
                profileName.equalsIgnoreCase(assessment.getProfileName()))
            .toList();

        supersededAssessments.forEach(assessment ->
            dataQualityAssessmentIndexRepository.deleteById(String.valueOf(assessment.getId())));

        revision.getAssessments().removeAll(supersededAssessments);

        entityRevisionRepository.save(revision);

        applicationEventPublisher.publishEvent(new DataQualityAssessmentEvent(
            revision, CompressionUtil.decompress(revision.getCompressedContent()), profileName));

        log.info("Dropped assessments of revision {}.{} of entity '{}' (ID={}), reassessment " +
                "scheduled.", revision.getMajorVersion(), revision.getMinorVersion(), entityType,
            entityId);

        return true;
    }

    private record PendingIssue(DataQualityAssessmentIndex assessment, String ruleKey,
                                IssueSeverity severity, String entityType) {

        IssueCursor cursor() {
            return new IssueCursor(assessment.getEntityId(), severity, ruleKey, entityType,
                Integer.valueOf(assessment.getId()));
        }
    }

    private record IssueWindow(List<DataQualityIssueDTO> issues, @Nullable String nextCursor) {
    }
}
