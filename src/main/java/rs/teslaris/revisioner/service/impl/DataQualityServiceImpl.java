package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.json.JsonData;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.institution.OrganisationUnitService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.converter.DataQualityAssessmentConverter;
import rs.teslaris.revisioner.converter.DataQualityProfileConverter;
import rs.teslaris.revisioner.converter.IssueConverter;
import rs.teslaris.revisioner.converter.IssueDetailsConverter;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDetailsDTO;
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
import rs.teslaris.revisioner.util.dataquality.RelatedEntityType;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityServiceImpl implements DataQualityService {

    private static final String DOCUMENT_TARGET = "Document";

    private static final String ACTIVITY_TARGET = "Activity";

    private static final int ISSUE_SCAN_BATCH_SIZE = 500;

    private static final List<String> DOCUMENT_PERSON_ROLE_FIELDS = List.of(
        "author_ids", "editor_ids", "reviewer_ids", "board_member_ids", "advisor_ids",
        "presenter_ids", "translator_ids", "assistant_staff_ids", "arguer_ids", "owner_ids",
        "associated_editor_ids", "invited_editor_ids"
    );

    private static final Comparator<DataQualityIssueDTO> ISSUE_ORDER =
        Comparator
            .comparingInt((DataQualityIssueDTO issue) -> issue.severity().ordinal())
            .thenComparing(DataQualityIssueDTO::ruleKey)
            .thenComparing(DataQualityIssueDTO::entityType);

    private static final String ISSUE_INDEX_NAME = "data_quality_assessment";

    private final EntityRevisionRepository entityRevisionRepository;

    private final DataQualityAssessmentRepository dataQualityAssessmentRepository;

    private final LanguageTagService languageTagService;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final SearchService<DataQualityAssessmentIndex> searchService;

    private final ApplicationEventPublisher applicationEventPublisher;

    private final DataQualityAggregator dataQualityAggregator;

    private final OrganisationUnitService organisationUnitService;


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
            return List.of(
                RelatedQualityDTO.unsupported(RelatedEntityType.OUTPUTS),
                RelatedQualityDTO.unsupported(RelatedEntityType.PROJECTS),
                RelatedQualityDTO.unsupported(RelatedEntityType.ACTIVITIES),
                RelatedQualityDTO.unsupported(RelatedEntityType.FUNDINGS)
            );
        }

        // An organisation unit answers for everything below it, and not every record carries its
        // ancestors: a thesis is indexed under its own institution only, and person, event and
        // organisation unit assessments under their direct one. Expanding the sub-hierarchy here
        // makes the scope hold whatever the indexer stored.
        var scopeIds = organisationUnitScope(isOrganisationUnit, entityId);

        var assessments = dataQualityAggregator
            .aggregateAssessments(
                relatedAssessmentsQuery(isPerson, entityId, scopeIds, profileName),
                expandRuleKeys(profileName, ACTIVITY_TARGET, null, null, null))
            .orElseGet(DataQualityAggregator.AssessmentAggregates::empty);

        var documents = dataQualityAggregator
            .aggregateLinkedDocuments(linkedDocumentsQuery(isPerson, entityId, scopeIds))
            .orElseGet(DataQualityAggregator.LinkedDocumentAggregates::empty);

        return List.of(
            new RelatedQualityDTO(
                RelatedEntityType.OUTPUTS,
                documents.linkedRecords(),
                assessments.affectedRecords(),
                assessments.openIssues() - assessments.activityIssues(),
                assessments.averageScore(),
                true
            ),
            // TODO: projects have no quality assessments yet.
            RelatedQualityDTO.unsupported(RelatedEntityType.PROJECTS),
            // Activities live on other records, so both figures are sums of activity counters, and
            // there is no score because the Activity target is never scored.
            new RelatedQualityDTO(
                RelatedEntityType.ACTIVITIES,
                documents.linkedActivities(),
                assessments.activitiesCount(),
                assessments.activityIssues(),
                null,
                true
            ),
            // TODO: fundings have no quality assessments yet.
            RelatedQualityDTO.unsupported(RelatedEntityType.FUNDINGS)
        );
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
    public Page<DataQualityIssueDTO> findIssuesForEntity(String entityType, Integer entityId,
                                                         String profileName, String target,
                                                         QualityDimension dimension,
                                                         IssueSeverity severity,
                                                         String constraintKey, Pageable pageable) {
        var query = buildIssueQuery(entityType, entityId,
            organisationUnitScope(EntityType.ORGANISATION_UNIT.name().equals(entityType), entityId),
            profileName, ACTIVITY_TARGET.equals(target) ? DOCUMENT_TARGET : target);

        var window = collectIssueWindow(query, target, dimension, severity, constraintKey,
            (int) pageable.getOffset(), pageable.getPageSize());

        var totalIssues = countIssues(query, profileName, target, dimension, severity,
            constraintKey, window.scannedIssues());

        return new PageImpl<>(window.issues(), pageable, totalIssues);
    }

    private IssueWindow collectIssueWindow(Query query, String target, QualityDimension dimension,
                                           IssueSeverity severity, String constraintKey,
                                           int offset, int pageSize) {
        var window = new ArrayList<DataQualityIssueDTO>();
        var block = new ArrayList<DataQualityIssueDTO>();

        var emittedAtWatermark = new HashSet<String>();
        Integer watermark = null;
        Integer blockEntityId = null;
        var scanned = 0;

        while (true) {
            var batch = searchService.runQuery(
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
                    scanned += flushBlock(block, window, offset, pageSize, scanned);
                    blockEntityId = assessment.getEntityId();
                }

                if (!Objects.equals(watermark, assessment.getEntityId())) {
                    watermark = assessment.getEntityId();
                    emittedAtWatermark.clear();
                    emittedAtWatermark.add(assessment.getId());
                }

                expandIssues(assessment, target, dimension, severity, constraintKey, block);
            }

            if (window.size() >= pageSize) {
                // The window is complete; everything still unscanned sorts after it. The scanned
                // count is only a fallback for the exact aggregated total, so a lower bound is fine.
                return new IssueWindow(window, scanned);
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

        scanned += flushBlock(block, window, offset, pageSize, scanned);

        return new IssueWindow(window, scanned);
    }

    private int flushBlock(List<DataQualityIssueDTO> block, List<DataQualityIssueDTO> window,
                           int offset, int pageSize, int scanned) {
        if (block.isEmpty()) {
            return 0;
        }

        block.sort(ISSUE_ORDER);

        for (var issue : block) {
            if (scanned >= offset && window.size() < pageSize) {
                window.add(issue);
            }

            scanned++;
        }

        var contributed = block.size();
        block.clear();

        return contributed;
    }

    private void expandIssues(DataQualityAssessmentIndex assessment, String target,
                              QualityDimension dimension, IssueSeverity severity,
                              String constraintKey, List<DataQualityIssueDTO> collector) {
        var applicableKeys = DataQualityAssessmentConfigurationLoader.listRuleKeys(
            assessment.getProfileName(), assessment.getProfileVersion(), target, dimension,
            severity);

        Objects.requireNonNullElse(assessment.getFailedRuleKeys(), List.<String>of()).stream()
            .filter(applicableKeys::contains)
            .filter(ruleKey -> Objects.isNull(constraintKey) || constraintKey.equals(ruleKey))
            .forEach(ruleKey -> collector.add(IssueConverter.toDTO(assessment, ruleKey)));
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

    private Query buildIssueQuery(String entityType, Integer entityId, List<Integer> scopeIds,
                                  String profileName, String target) {
        var isPerson = EntityType.PERSON.name().equals(entityType);

        var relatedClause = isPerson
            ? TermQuery.of(tq -> tq.field("related_person_ids").value(entityId))._toQuery()
            : termsQuery("organisation_unit_ids", scopeIds);

        return BoolQuery.of(topLevel -> topLevel
            .must(scope -> scope.bool(b -> b
                .should(own -> own.bool(ownEntity -> ownEntity
                    .must(m -> m.term(tq -> tq.field("entity_type").value(entityType)))
                    .must(m -> m.term(tq -> tq.field("entity_id").value(entityId)))))
                .should(relatedClause)
                .minimumShouldMatch("1")))
            .must(m -> m.term(tq -> tq.field("is_latest").value(true)))
            .must(m -> m.term(tq -> tq.field("profile_name").value(profileName)))
            .must(m -> Objects.isNull(target)
                ? m.matchAll(ma -> ma)
                : m.term(tq -> tq.field("target").value(target)))
        )._toQuery();
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

    private record IssueWindow(List<DataQualityIssueDTO> issues, int scannedIssues) {
    }
}
