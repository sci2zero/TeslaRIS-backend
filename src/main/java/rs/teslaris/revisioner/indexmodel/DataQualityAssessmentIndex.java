package rs.teslaris.revisioner.indexmodel;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import rs.teslaris.revisioner.model.qualityassessment.QualityDimension;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "data_quality_assessment")
@Setting(settingPath = "/configuration/data-quality-assessment-index-config.json")
public class DataQualityAssessmentIndex {

    public static final LocalDateTime OPEN_INTERVAL_END =
        LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Id
    private String id;

    @Field(type = FieldType.Keyword, store = true, name = "entity_type")
    private String entityType;

    @JsonAlias("entity_name_sr")
    @Field(type = FieldType.Text, name = "entity_name_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String entityNameSr;

    @JsonAlias("entity_name_other")
    @Field(type = FieldType.Text, name = "entity_name_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String entityNameOther;

    @Field(type = FieldType.Keyword, store = true, name = "target")
    private String target;

    @Field(type = FieldType.Integer, store = true, name = "entity_id")
    private Integer entityId;

    @Field(type = FieldType.Integer, store = true, name = "related_person_ids")
    private List<Integer> relatedPersonIds;

    @Field(type = FieldType.Integer, store = true, name = "organisation_unit_ids")
    private List<Integer> organisationUnitIds;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis,
        store = true, name = "assessment_date")
    private LocalDateTime assessmentDate;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis,
        store = true, name = "valid_to")
    private LocalDateTime validTo;

    @Field(type = FieldType.Integer, store = true, name = "record_major_version")
    private Integer recordMajorVersion;

    @Field(type = FieldType.Integer, store = true, name = "record_minor_version")
    private Integer recordMinorVersion;

    @Field(type = FieldType.Boolean, store = true, name = "is_latest")
    private boolean isLatest;

    @Field(type = FieldType.Keyword, store = true, name = "profile_name")
    private String profileName;

    @Field(type = FieldType.Keyword, store = true, name = "profile_version")
    private String profileVersion;

    @Field(type = FieldType.Boolean, store = true, name = "valid")
    private boolean valid;

    @Field(type = FieldType.Double, store = true, name = "quality_score")
    private double qualityScore;

    @Field(type = FieldType.Double, store = true, name = "quality_score_fair")
    private double qualityScoreFair;

    @Field(type = FieldType.Integer, store = true, name = "passed_rules")
    private int passedRules;

    @Field(type = FieldType.Integer, store = true, name = "info_failed_rules")
    private int infoFailedRules;

    @Field(type = FieldType.Integer, store = true, name = "activities_count")
    private Integer activitiesCount;

    @Field(type = FieldType.Integer, store = true, name = "activity_publication_candidates_count")
    private Integer activityPublicationCandidatesCount;

    @Field(type = FieldType.Double, store = true, name = "activity_score_sum")
    private Double activityScoreSum;

    @Field(type = FieldType.Integer, store = true, name = "activity_error_issues")
    private Integer activityErrorIssues;

    @Field(type = FieldType.Integer, store = true, name = "activity_warning_issues")
    private Integer activityWarningIssues;

    @Field(type = FieldType.Integer, store = true, name = "activity_info_issues")
    private Integer activityInfoIssues;

    @Field(type = FieldType.Object, name = "activity_dimension_score_sums")
    private Map<QualityDimension, Double> activityDimensionScoreSums = new HashMap<>();

    @Field(type = FieldType.Double, store = true, name = "activity_fair_score_sum")
    private Double activityFairScoreSum;

    @Field(type = FieldType.Object, name = "activity_issue_occurrences")
    private Map<String, Integer> activityIssueOccurrences = new HashMap<>();

    @Field(type = FieldType.Integer, store = true, name = "warning_failed_rules")
    private int warningFailedRules;

    @Field(type = FieldType.Integer, store = true, name = "error_failed_rules")
    private int errorFailedRules;

    @Field(type = FieldType.Integer, store = true, name = "blocking_failed_rules")
    private int blockingFailedRules;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;

    @Field(type = FieldType.Boolean, store = true, name = "publication_candidate")
    private boolean publicationCandidate;

    // --- rule-centric reporting: plain key lists, aggregatable via terms (no nested needed) ---

    @Field(type = FieldType.Keyword, store = true, name = "failed_rule_keys")
    private List<String> failedRuleKeys;

    @Field(type = FieldType.Keyword, store = true, name = "passed_rule_keys")
    private List<String> passedRuleKeys;

    @Field(type = FieldType.Keyword, store = true, name = "blocking_rule_keys")
    private List<String> blockingRuleKeys;

    // --- one {score, issue_count, passed_count, fair_score} quadruple per QualityDimension value ---

    @Field(type = FieldType.Double, store = true, name = "accuracy_score")
    private double accuracyScore;
    @Field(type = FieldType.Integer, store = true, name = "accuracy_issue_count")
    private int accuracyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "accuracy_passed_count")
    private int accuracyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "accuracy_fair_score")
    private double accuracyFairScore;

    @Field(type = FieldType.Double, store = true, name = "consistency_score")
    private double consistencyScore;
    @Field(type = FieldType.Integer, store = true, name = "consistency_issue_count")
    private int consistencyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "consistency_passed_count")
    private int consistencyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "consistency_fair_score")
    private double consistencyFairScore;

    @Field(type = FieldType.Double, store = true, name = "lineage_score")
    private double lineageScore;
    @Field(type = FieldType.Integer, store = true, name = "lineage_issue_count")
    private int lineageIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "lineage_passed_count")
    private int lineagePassedCount;
    @Field(type = FieldType.Double, store = true, name = "lineage_fair_score")
    private double lineageFairScore;

    @Field(type = FieldType.Double, store = true, name = "structural_consistency_score")
    private double structuralConsistencyScore;
    @Field(type = FieldType.Integer, store = true, name = "structural_consistency_issue_count")
    private int structuralConsistencyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "structural_consistency_passed_count")
    private int structuralConsistencyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "structural_consistency_fair_score")
    private double structuralConsistencyFairScore;

    @Field(type = FieldType.Double, store = true, name = "qualitative_score")
    private double qualitativeScore;
    @Field(type = FieldType.Integer, store = true, name = "qualitative_issue_count")
    private int qualitativeIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "qualitative_passed_count")
    private int qualitativePassedCount;
    @Field(type = FieldType.Double, store = true, name = "qualitative_fair_score")
    private double qualitativeFairScore;

    @Field(type = FieldType.Double, store = true, name = "semantic_score")
    private double semanticScore;
    @Field(type = FieldType.Integer, store = true, name = "semantic_issue_count")
    private int semanticIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "semantic_passed_count")
    private int semanticPassedCount;
    @Field(type = FieldType.Double, store = true, name = "semantic_fair_score")
    private double semanticFairScore;

    @Field(type = FieldType.Double, store = true, name = "currency_score")
    private double currencyScore;
    @Field(type = FieldType.Integer, store = true, name = "currency_issue_count")
    private int currencyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "currency_passed_count")
    private int currencyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "currency_fair_score")
    private double currencyFairScore;
}
