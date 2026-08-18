package rs.teslaris.revisioner.indexmodel;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "data_quality_assessment")
@Setting(settingPath = "/configuration/index-config.json")
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

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis,
        store = true, name = "superseded_at")
    private LocalDateTime supersededAt;

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

    @Field(type = FieldType.Integer, store = true, name = "warning_failed_rules")
    private int warningFailedRules;

    @Field(type = FieldType.Integer, store = true, name = "error_failed_rules")
    private int errorFailedRules;

    // --- rule-centric reporting: plain key lists, aggregatable via terms (no nested needed) ---

    @Field(type = FieldType.Keyword, store = true, name = "failed_rule_keys")
    private List<String> failedRuleKeys;

    @Field(type = FieldType.Keyword, store = true, name = "passed_rule_keys")
    private List<String> passedRuleKeys;

    // --- one {score, issue_count, passed_count, fair_score} quadruple per QualityDimension value ---

    @Field(type = FieldType.Double, store = true, name = "completeness_score")
    private double completenessScore;
    @Field(type = FieldType.Integer, store = true, name = "completeness_issue_count")
    private int completenessIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "completeness_passed_count")
    private int completenessPassedCount;
    @Field(type = FieldType.Double, store = true, name = "completeness_fair_score")
    private double completenessFairScore;

    @Field(type = FieldType.Double, store = true, name = "validity_score")
    private double validityScore;
    @Field(type = FieldType.Integer, store = true, name = "validity_issue_count")
    private int validityIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "validity_passed_count")
    private int validityPassedCount;
    @Field(type = FieldType.Double, store = true, name = "validity_fair_score")
    private double validityFairScore;

    @Field(type = FieldType.Double, store = true, name = "uniqueness_score")
    private double uniquenessScore;
    @Field(type = FieldType.Integer, store = true, name = "uniqueness_issue_count")
    private int uniquenessIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "uniqueness_passed_count")
    private int uniquenessPassedCount;
    @Field(type = FieldType.Double, store = true, name = "uniqueness_fair_score")
    private double uniquenessFairScore;

    @Field(type = FieldType.Double, store = true, name = "consistency_score")
    private double consistencyScore;
    @Field(type = FieldType.Integer, store = true, name = "consistency_issue_count")
    private int consistencyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "consistency_passed_count")
    private int consistencyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "consistency_fair_score")
    private double consistencyFairScore;

    @Field(type = FieldType.Double, store = true, name = "timeliness_score")
    private double timelinessScore;
    @Field(type = FieldType.Integer, store = true, name = "timeliness_issue_count")
    private int timelinessIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "timeliness_passed_count")
    private int timelinessPassedCount;
    @Field(type = FieldType.Double, store = true, name = "timeliness_fair_score")
    private double timelinessFairScore;

    @Field(type = FieldType.Double, store = true, name = "accuracy_score")
    private double accuracyScore;
    @Field(type = FieldType.Integer, store = true, name = "accuracy_issue_count")
    private int accuracyIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "accuracy_passed_count")
    private int accuracyPassedCount;
    @Field(type = FieldType.Double, store = true, name = "accuracy_fair_score")
    private double accuracyFairScore;

    @Field(type = FieldType.Double, store = true, name = "conformity_score")
    private double conformityScore;
    @Field(type = FieldType.Integer, store = true, name = "conformity_issue_count")
    private int conformityIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "conformity_passed_count")
    private int conformityPassedCount;
    @Field(type = FieldType.Double, store = true, name = "conformity_fair_score")
    private double conformityFairScore;

    @Field(type = FieldType.Double, store = true, name = "integrity_score")
    private double integrityScore;
    @Field(type = FieldType.Integer, store = true, name = "integrity_issue_count")
    private int integrityIssueCount;
    @Field(type = FieldType.Integer, store = true, name = "integrity_passed_count")
    private int integrityPassedCount;
    @Field(type = FieldType.Double, store = true, name = "integrity_fair_score")
    private double integrityFairScore;
}
