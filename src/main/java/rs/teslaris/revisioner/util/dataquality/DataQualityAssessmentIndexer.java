package rs.teslaris.revisioner.util.dataquality;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.EventDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.revisioner.indexmodel.DataQualityAssessmentIndex;
import rs.teslaris.revisioner.indexrepository.DataQualityAssessmentIndexRepository;
import rs.teslaris.revisioner.model.qualityassessment.ConstraintEvaluationResult;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataQualityAssessmentIndexer {

    private static final String TARGET_PERSON = "Person";

    private static final String TARGET_DOCUMENT = "Document";

    private static final String TARGET_EVENT = "Event";

    private static final String TARGET_ORGANISATION_UNIT = "OrganisationUnit";

    private static final String TARGET_ACTIVITY = "Activity";

    private final DataQualityAssessmentIndexRepository indexRepository;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final EventIndexRepository eventIndexRepository;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;


    public void index(DataQualityAssessment assessment, List<String> targets, Object dto) {
        var target = targets.getFirst();
        try {
            var revision = assessment.getRevision();
            var entityType = revision.getEntityType();
            var entityId = revision.getEntityId();
            var assessmentDate =
                LocalDateTime.ofInstant(assessment.getFinishedAt(), ZoneOffset.UTC);

            var index = new DataQualityAssessmentIndex();
            index.setId(String.valueOf(assessment.getId()));
            index.setEntityType(entityType);
            index.setTarget(target);
            index.setEntityId(entityId);
            index.setRelatedPersonIds(resolveRelatedPersonIds(target, entityId, dto));
            index.setOrganisationUnitIds(resolveOrganisationUnitIds(target, entityId, dto));
            index.setAssessmentDate(assessmentDate);
            index.setValidTo(DataQualityAssessmentIndex.OPEN_INTERVAL_END);
            index.setSupersededAt(null);
            index.setLatest(true);
            index.setRecordMajorVersion(revision.getMajorVersion());
            index.setRecordMinorVersion(revision.getMinorVersion());
            index.setProfileName(assessment.getProfileName());
            index.setProfileVersion(assessment.getProfileVersion());
            index.setValid(assessment.getValid());
            index.setQualityScore(assessment.getQualityScore());
            index.setQualityScoreFair(assessment.getQualityScoreFair());
            index.setPassedRules(assessment.getPassedRules());
            index.setInfoFailedRules(assessment.getInfoFailedRules());
            index.setBlockingFailedRules(assessment.getBlockingFailedRules());
            index.setWarningFailedRules(assessment.getWarningFailedRules());
            index.setErrorFailedRules(assessment.getErrorFailedRules());
            index.setActivitiesCount(assessment.getActivitiesCount());
            index.setActivityPublicationCandidatesCount(
                assessment.getActivityPublicationCandidatesCount());
            index.setActivityScoreSum(assessment.getActivityScoreSum());
            index.setActivityErrorIssues(assessment.getActivityErrorIssues());
            index.setActivityWarningIssues(assessment.getActivityWarningIssues());
            index.setActivityInfoIssues(assessment.getActivityInfoIssues());
            index.setActivityDimensionScoreSums(
                new HashMap<>(assessment.getActivityDimensionScoreSums()));
            index.setActivityFairScoreSum(assessment.getActivityFairScoreSum());
            index.setDatabaseId(assessment.getId());
            index.setPublicationCandidate(assessment.getPublicationCandidate());

            populateRuleKeys(index, assessment, targets);
            populateDimensionBreakdown(index, assessment, targets);

            supersedePreviousLatest(entityType, entityId, assessment.getProfileName(),
                assessmentDate);

            setEntityName(index);

            indexRepository.save(index);
        } catch (Exception e) {
            log.error(
                "Failed to index data quality assessment {} into Elasticsearch.",
                assessment.getId(), e
            );
        }
    }

    private void supersedePreviousLatest(String entityType, Integer entityId, String profileName,
                                         LocalDateTime newAssessmentDate) {
        indexRepository
            .findByEntityTypeAndEntityIdAndProfileNameAndIsLatestTrue(entityType, entityId,
                profileName)
            .ifPresent(previous -> {
                previous.setSupersededAt(newAssessmentDate);
                previous.setValidTo(newAssessmentDate);
                previous.setLatest(false);
                indexRepository.save(previous);
            });
    }

    private List<Integer> resolveRelatedPersonIds(String target, Integer entityId, Object dto) {
        if (TARGET_PERSON.equals(target)) {
            return List.of(entityId);
        }

        if (TARGET_DOCUMENT.equals(target)) {
            var fromIndex = documentPublicationIndexRepository
                .findDocumentPublicationIndexByDatabaseId(entityId)
                .map(DocumentPublicationIndex::getAuthorIds)
                .filter(ids -> !ids.isEmpty());

            return fromIndex.orElseGet(() -> contributionPersonIds(dto));
        }

        if (TARGET_EVENT.equals(target)) {
            return contributionPersonIds(dto);
        }

        return List.of();
    }

    private void setEntityName(DataQualityAssessmentIndex index) {
        if (TARGET_PERSON.equals(index.getTarget())) {
            personIndexRepository.findByDatabaseId(index.getEntityId())
                .ifPresent(personIndex -> {
                    index.setEntityNameSr(personIndex.getName());
                    index.setEntityNameOther(personIndex.getName());
                });
        }

        if (TARGET_DOCUMENT.equals(index.getTarget())) {
            documentPublicationIndexRepository.findDocumentPublicationIndexByDatabaseId(
                    index.getEntityId())
                .ifPresent(documentIndex -> {
                    index.setEntityNameSr(documentIndex.getTitleSr());
                    index.setEntityNameOther(documentIndex.getTitleOther());
                });
        }

        if (TARGET_EVENT.equals(index.getTarget())) {
            eventIndexRepository.findByDatabaseId(index.getEntityId())
                .ifPresent(eventIndex -> {
                    index.setEntityNameSr(eventIndex.getNameSr());
                    index.setEntityNameOther(eventIndex.getNameOther());
                });
        }

        if (TARGET_ORGANISATION_UNIT.equals(index.getTarget())) {
            organisationUnitIndexRepository.findOrganisationUnitIndexByDatabaseId(
                    index.getEntityId())
                .ifPresent(organisationUnitIndex -> {
                    index.setEntityNameSr(organisationUnitIndex.getNameSr());
                    index.setEntityNameOther(organisationUnitIndex.getNameOther());
                });
        }
    }

    private List<Integer> contributionPersonIds(Object dto) {
        if (dto instanceof DocumentDTO document) {
            return extractPersonIds(document.getContributions());
        }

        if (dto instanceof EventDTO event) {
            return extractPersonIds(event.getContributions());
        }

        return List.of();
    }

    private List<Integer> extractPersonIds(List<? extends PersonContributionDTO> contributions) {
        return contributions.stream()
            .map(PersonContributionDTO::getPersonId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private List<Integer> resolveOrganisationUnitIds(String target, Integer entityId, Object dto) {
        if (TARGET_ORGANISATION_UNIT.equals(target)) {
            return List.of(entityId);
        }

        if (TARGET_PERSON.equals(target)) {
            return personIndexRepository.findByDatabaseId(entityId)
                .map(PersonIndex::getEmploymentInstitutionsId)
                .orElseGet(List::of);
        }

        if (TARGET_DOCUMENT.equals(target)) {
            var fromIndex = documentPublicationIndexRepository
                .findDocumentPublicationIndexByDatabaseId(entityId)
                .map(DocumentPublicationIndex::getOrganisationUnitIds)
                .filter(ids -> !ids.isEmpty());

            return fromIndex.orElseGet(() -> contributionInstitutionIds(dto));
        }

        if (TARGET_EVENT.equals(target)) {
            return contributionInstitutionIds(dto);
        }

        return List.of();
    }

    private List<Integer> contributionInstitutionIds(Object dto) {
        return institutionIdsForContributors(contributionPersonIds(dto));
    }

    private List<Integer> institutionIdsForContributors(List<Integer> contributorPersonIds) {
        if (contributorPersonIds.isEmpty()) {
            return List.of();
        }

        var institutionIds = new HashSet<Integer>();
        personIndexRepository
            .findByDatabaseIdIn(contributorPersonIds,
                PageRequest.of(0, contributorPersonIds.size()))
            .forEach(
                personIndex -> institutionIds.addAll(personIndex.getEmploymentInstitutionsId()));

        return List.copyOf(institutionIds);
    }

    private void populateRuleKeys(DataQualityAssessmentIndex index,
                                  DataQualityAssessment assessment, List<String> targets) {
        var rulesForTarget = DataQualityAssessmentConfigurationLoader.getRulesForTarget(
            assessment.getProfileName(), assessment.getProfileVersion(), targets);

        var failedKeys = assessment.getIssues().stream()
            .map(ConstraintEvaluationResult::getKey)
            .collect(Collectors.toSet());

        // A rule that blocks publication is the one a candidate report cares about, so those keys
        // are kept apart rather than filtered out of the full set at query time.
        var blockingKeys = assessment.getIssues().stream()
            .filter(ConstraintEvaluationResult::isBlocking)
            .map(ConstraintEvaluationResult::getKey)
            .collect(Collectors.toSet());

        var passedKeys = rulesForTarget.stream()
            .map(Map.Entry::getKey)
            .filter(key -> !failedKeys.contains(key))
            .toList();

        index.setFailedRuleKeys(new ArrayList<>(failedKeys));
        index.setBlockingRuleKeys(new ArrayList<>(blockingKeys));
        index.setPassedRuleKeys(passedKeys);

        populateActivityIssueOccurrences(index, assessment);
    }

    private void populateActivityIssueOccurrences(DataQualityAssessmentIndex index,
                                                  DataQualityAssessment assessment) {
        var activityRuleKeys = DataQualityAssessmentConfigurationLoader.listRuleKeys(
            assessment.getProfileName(), assessment.getProfileVersion(), TARGET_ACTIVITY,
            null, null
        );

        if (activityRuleKeys.isEmpty()) {
            return;
        }

        index.setActivityIssueOccurrences(assessment.getIssues().stream()
            .map(ConstraintEvaluationResult::getKey)
            .filter(activityRuleKeys::contains)
            .collect(Collectors.groupingBy(
                key -> key,
                HashMap::new,
                Collectors.summingInt(key -> 1))));
    }

    private void populateDimensionBreakdown(DataQualityAssessmentIndex index,
                                            DataQualityAssessment assessment,
                                            List<String> targets) {
        var profile = assessment.getProfileName();
        var version = assessment.getProfileVersion();

        var issuesByDimension = assessment.getIssues().stream()
            .collect(Collectors.groupingBy(ConstraintEvaluationResult::getDimension));

        var rulesForTarget =
            DataQualityAssessmentConfigurationLoader.getRulesForTarget(profile, version, targets);

        for (var dimension : QualityDimension.values()) {
            var dimensionScore = assessment.getDimensionScores().get(dimension);
            var score = Objects.nonNull(dimensionScore) ? dimensionScore.percentage() : 0.0;

            var issueCount = issuesByDimension.getOrDefault(dimension, List.of()).size();

            var totalRuleCount = (int) rulesForTarget.stream()
                .filter(entry -> entry.getValue().dimension() == dimension)
                .count();
            var passedCount = Math.max(0, totalRuleCount - issueCount);

            var fairScore =
                computeFairScoreForDimension(profile, version, rulesForTarget, dimension,
                    assessment);

            applyDimensionBreakdown(index, dimension, score, issueCount, passedCount, fairScore);
        }
    }

    private double computeFairScoreForDimension(String profile, String version,
                                                List<Map.Entry<String, DataQualityAssessmentConfigurationLoader.DataQualityRemark>> rulesForTarget,
                                                QualityDimension dimension,
                                                DataQualityAssessment assessment) {

        var fairRulesInDimension = rulesForTarget.stream()
            .filter(entry -> entry.getValue().dimension() == dimension &&
                entry.getValue().usedForFairCompliance())
            .toList();

        if (fairRulesInDimension.isEmpty()) {
            return 100.0;
        }

        double totalFairPoints = fairRulesInDimension.stream()
            .mapToDouble(entry -> DataQualityAssessmentConfigurationLoader.getWeightedPoints(
                profile, version, entry.getValue()))
            .sum();

        Set<String> fairKeysInDimension =
            fairRulesInDimension.stream().map(Map.Entry::getKey).collect(Collectors.toSet());

        double deductedFairPoints = assessment.getIssues().stream()
            .filter(issue -> fairKeysInDimension.contains(issue.getKey()))
            .mapToDouble(issue -> {
                var remark = DataQualityAssessmentConfigurationLoader.getIssue(profile, version,
                    issue.getKey());
                return DataQualityAssessmentConfigurationLoader.getWeightedPoints(profile, version,
                    remark);
            })
            .sum();

        return totalFairPoints == 0
            ? 100.0
            : (totalFairPoints - deductedFairPoints) / totalFairPoints * 100.0;
    }

    private void applyDimensionBreakdown(DataQualityAssessmentIndex index,
                                         QualityDimension dimension, double score, int issueCount,
                                         int passedCount, double fairScore) {
        switch (dimension) {
            case COMPLETENESS -> {
                index.setCompletenessScore(score);
                index.setCompletenessIssueCount(issueCount);
                index.setCompletenessPassedCount(passedCount);
                index.setCompletenessFairScore(fairScore);
            }
            case VALIDITY -> {
                index.setValidityScore(score);
                index.setValidityIssueCount(issueCount);
                index.setValidityPassedCount(passedCount);
                index.setValidityFairScore(fairScore);
            }
            case UNIQUENESS -> {
                index.setUniquenessScore(score);
                index.setUniquenessIssueCount(issueCount);
                index.setUniquenessPassedCount(passedCount);
                index.setUniquenessFairScore(fairScore);
            }
            case CONSISTENCY -> {
                index.setConsistencyScore(score);
                index.setConsistencyIssueCount(issueCount);
                index.setConsistencyPassedCount(passedCount);
                index.setConsistencyFairScore(fairScore);
            }
            case TIMELINESS -> {
                index.setTimelinessScore(score);
                index.setTimelinessIssueCount(issueCount);
                index.setTimelinessPassedCount(passedCount);
                index.setTimelinessFairScore(fairScore);
            }
            case ACCURACY -> {
                index.setAccuracyScore(score);
                index.setAccuracyIssueCount(issueCount);
                index.setAccuracyPassedCount(passedCount);
                index.setAccuracyFairScore(fairScore);
            }
            case CONFORMITY -> {
                index.setConformityScore(score);
                index.setConformityIssueCount(issueCount);
                index.setConformityPassedCount(passedCount);
                index.setConformityFairScore(fairScore);
            }
            case INTEGRITY -> {
                index.setIntegrityScore(score);
                index.setIntegrityIssueCount(issueCount);
                index.setIntegrityPassedCount(passedCount);
                index.setIntegrityFairScore(fairScore);
            }
        }
    }
}
