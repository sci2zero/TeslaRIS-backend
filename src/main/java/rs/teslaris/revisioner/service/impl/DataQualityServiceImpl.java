package rs.teslaris.revisioner.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.commontypes.LanguageTagService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.functional.Pair;
import rs.teslaris.revisioner.converter.DataQualityAssessmentConverter;
import rs.teslaris.revisioner.converter.DataQualityProfileConverter;
import rs.teslaris.revisioner.converter.IssueConverter;
import rs.teslaris.revisioner.dto.DataQualityAssessmentDTO;
import rs.teslaris.revisioner.dto.DataQualityIssueDTO;
import rs.teslaris.revisioner.dto.DataQualityProfileDTO;
import rs.teslaris.revisioner.dto.ProfileRelatedQualityDTO;
import rs.teslaris.revisioner.dto.QualityReportResponseDTO;
import rs.teslaris.revisioner.dto.RelatedQualityDTO;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.DataQualityAssessmentEvent;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.IssueSeverity;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;
import rs.teslaris.revisioner.repository.EntityRevisionRepository;
import rs.teslaris.revisioner.service.interfaces.DataQualityService;
import rs.teslaris.revisioner.util.CompressionUtil;
import rs.teslaris.revisioner.util.dataquality.DataQualityAssessmentConfigurationLoader;
import rs.teslaris.revisioner.util.dataquality.RelatedEntityType;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataQualityServiceImpl implements DataQualityService {

    private static final String DOCUMENT_TARGET = "Document";

    private static final String ACTIVITY_TARGET = "Activity";

    private static final int ASSESSMENT_BATCH_SIZE = 500;

    private static final int ISSUE_SEARCH_BATCH_SIZE = 10000;

    private static final String ISSUE_INDEX_NAME = "data_quality_assessment";

    private final EntityRevisionRepository entityRevisionRepository;

    private final LanguageTagService languageTagService;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final DataQualityAssessmentIndexRepository dataQualityAssessmentIndexRepository;

    private final SearchService<DataQualityAssessmentIndex> searchService;

    private final ApplicationEventPublisher applicationEventPublisher;


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
                List.of(
                    outputsQuality(entityType, entityId, assessment.getProfileName()),
                    // TODO: projects have no quality assessments yet.
                    RelatedQualityDTO.unsupported(RelatedEntityType.PROJECTS),
                    activitiesQuality(entityType, entityId, assessment.getProfileName()),
                    // TODO: fundings have no quality assessments yet.
                    RelatedQualityDTO.unsupported(RelatedEntityType.FUNDINGS)
                )))
            .toList()).orElseGet(List::of);
    }

    private RelatedQualityDTO activitiesQuality(String entityType, Integer entityId,
                                                String profileName) {
        var isPerson = EntityType.PERSON.name().equals(entityType);
        var isOrganisationUnit = EntityType.ORGANISATION_UNIT.name().equals(entityType);

        if (!isPerson && !isOrganisationUnit) {
            return RelatedQualityDTO.unsupported(RelatedEntityType.ACTIVITIES);
        }

        long linkedActivities = 0;

        var documentPageable = PageRequest.of(0, ASSESSMENT_BATCH_SIZE);
        Page<DocumentPublicationIndex> documentPage;

        do {
            documentPage = isPerson
                ? documentPublicationIndexRepository.findLinkedToPerson(entityId, documentPageable)
                : documentPublicationIndexRepository.findLinkedToOrganisationUnit(entityId,
                documentPageable);

            for (var document : documentPage.getContent()) {
                linkedActivities += Objects.requireNonNullElse(document.getActivitiesCount(), 0);
            }

            documentPageable = documentPageable.next();
        } while (documentPage.hasNext());

        long affectedActivities = 0;
        long openIssues = 0;

        var activityRuleKeysPerVersion = new HashMap<String, Set<String>>();

        var pageable = PageRequest.of(0, ASSESSMENT_BATCH_SIZE);
        Page<DataQualityAssessmentIndex> page;

        do {
            page = isPerson
                ? dataQualityAssessmentIndexRepository
                .findByTargetAndProfileNameAndIsLatestTrueAndRelatedPersonIds(
                    DOCUMENT_TARGET, profileName, entityId, pageable)
                : dataQualityAssessmentIndexRepository
                .findByTargetAndProfileNameAndIsLatestTrueAndOrganisationUnitIds(
                    DOCUMENT_TARGET, profileName, entityId, pageable);

            for (var assessment : page.getContent()) {
                affectedActivities +=
                    Objects.requireNonNullElse(assessment.getActivitiesCount(), 0);
                openIssues += countIssuesForTarget(assessment, activityRuleKeysPerVersion);
            }

            pageable = pageable.next();
        } while (page.hasNext());

        return new RelatedQualityDTO(
            RelatedEntityType.ACTIVITIES,
            linkedActivities,
            affectedActivities,
            openIssues,
            null,
            true
        );
    }

    private long countIssuesForTarget(DataQualityAssessmentIndex assessment,
                                      Map<String, Set<String>> ruleKeysPerVersion) {
        var failedKeys =
            Objects.requireNonNullElse(assessment.getFailedRuleKeys(), List.<String>of());

        if (failedKeys.isEmpty()) {
            return 0;
        }

        var applicableKeys = ruleKeysPerVersion.computeIfAbsent(
            assessment.getProfileVersion(),
            version -> DataQualityAssessmentConfigurationLoader.listRuleKeys(
                assessment.getProfileName(), version, ACTIVITY_TARGET, null, null));

        return failedKeys.stream()
            .filter(applicableKeys::contains)
            .count();
    }

    private RelatedQualityDTO outputsQuality(String entityType, Integer entityId,
                                             String profileName) {
        var isPerson = EntityType.PERSON.name().equals(entityType);
        var isOrganisationUnit = EntityType.ORGANISATION_UNIT.name().equals(entityType);

        if (!isPerson && !isOrganisationUnit) {
            return RelatedQualityDTO.unsupported(RelatedEntityType.OUTPUTS);
        }

        var linkedRecords = isPerson
            ? documentPublicationIndexRepository.countLinkedToPerson(entityId)
            : documentPublicationIndexRepository.countLinkedToOrganisationUnit(entityId);

        long affectedRecords = 0;
        long openIssues = 0;
        double totalScore = 0;

        var pageable = PageRequest.of(0, ASSESSMENT_BATCH_SIZE);
        Page<DataQualityAssessmentIndex> page;

        do {
            page = isPerson
                ? dataQualityAssessmentIndexRepository
                .findByTargetAndProfileNameAndIsLatestTrueAndRelatedPersonIds(
                    DOCUMENT_TARGET, profileName, entityId, pageable)
                : dataQualityAssessmentIndexRepository
                .findByTargetAndProfileNameAndIsLatestTrueAndOrganisationUnitIds(
                    DOCUMENT_TARGET, profileName, entityId, pageable);

            for (var assessment : page.getContent()) {
                affectedRecords++;
                openIssues +=
                    assessment.getErrorFailedRules() + assessment.getWarningFailedRules() +
                        assessment.getInfoFailedRules();
                totalScore += assessment.getQualityScore();
            }

            pageable = pageable.next();
        } while (page.hasNext());

        return new RelatedQualityDTO(
            RelatedEntityType.OUTPUTS,
            Objects.requireNonNullElse(linkedRecords, 0L),
            affectedRecords,
            openIssues,
            affectedRecords > 0 ? totalScore / affectedRecords : null,
            true
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DataQualityIssueDTO> findIssuesForEntity(String entityType, Integer entityId,
                                                         String profileName, String target,
                                                         QualityDimension dimension,
                                                         IssueSeverity severity,
                                                         String constraintKey, Pageable pageable) {
        var query = buildIssueQuery(entityType, entityId, profileName,
            "Activity".equals(target) ? "Document" : target);

        var assessments = searchService.runQuery(
            query,
            Pageable.ofSize(ISSUE_SEARCH_BATCH_SIZE),
            DataQualityAssessmentIndex.class,
            ISSUE_INDEX_NAME
        );

        var issues = new ArrayList<DataQualityIssueDTO>();

        assessments.getContent().forEach(assessment -> {
            var applicableKeys = DataQualityAssessmentConfigurationLoader.listRuleKeys(
                assessment.getProfileName(), assessment.getProfileVersion(), target, dimension,
                severity);

            Objects.requireNonNullElse(assessment.getFailedRuleKeys(), List.<String>of()).stream()
                .filter(applicableKeys::contains)
                .filter(ruleKey -> Objects.isNull(constraintKey) || constraintKey.equals(ruleKey))
                .sorted()
                .forEach(ruleKey -> issues.add(IssueConverter.toDTO(assessment, ruleKey)));
        });

        issues.sort(
            Comparator
                .comparing(DataQualityIssueDTO::entityId)
                .thenComparingInt(issue -> issue.severity().ordinal())
                .thenComparing(DataQualityIssueDTO::ruleKey)
        );

        var from = (int) pageable.getOffset();

        if (from >= issues.size()) {
            return new PageImpl<>(List.of(), pageable, issues.size());
        }

        var to = Math.min(from + pageable.getPageSize(), issues.size());

        return new PageImpl<>(issues.subList(from, to), pageable, issues.size());
    }

    private Query buildIssueQuery(String entityType, Integer entityId, String profileName,
                                  String target) {
        var relatedField = EntityType.PERSON.name().equals(entityType)
            ? "related_person_ids" : "organisation_unit_ids";

        return BoolQuery.of(topLevel -> topLevel
            .must(scope -> scope.bool(b -> b
                .should(own -> own.bool(ownEntity -> ownEntity
                    .must(m -> m.term(tq -> tq.field("entity_type").value(entityType)))
                    .must(m -> m.term(tq -> tq.field("entity_id").value(entityId)))))
                .should(related -> related.term(
                    tq -> tq.field(relatedField).value(entityId)))
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
    public boolean reassessLatestRevision(String entityType, Integer entityId) {
        var latestRevision = entityRevisionRepository
            .findTopByEntityTypeAndEntityIdOrderByRevisionTimestampDesc(entityType, entityId);

        if (latestRevision.isEmpty()) {
            return false;
        }

        var revision = latestRevision.get();

        revision.getAssessments().forEach(assessment ->
            dataQualityAssessmentIndexRepository.deleteById(String.valueOf(assessment.getId())));

        revision.getAssessments().clear();

        entityRevisionRepository.save(revision);

        applicationEventPublisher.publishEvent(new DataQualityAssessmentEvent(
            revision, CompressionUtil.decompress(revision.getCompressedContent())));

        log.info("Dropped assessments of revision {}.{} of entity '{}' (ID={}), reassessment " +
                "scheduled.", revision.getMajorVersion(), revision.getMinorVersion(), entityType,
            entityId);

        return true;
    }
}
