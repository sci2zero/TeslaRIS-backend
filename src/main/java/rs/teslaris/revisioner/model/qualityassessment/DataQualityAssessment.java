package rs.teslaris.revisioner.model.qualityassessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;
import rs.teslaris.revisioner.model.EntityRevision;

@Entity
@Table(
    name = "data_quality_assessment",
    indexes = {
        @Index(
            name = "idx_dqa_revision_profile",
            columnList = "revision_id, profile_name, profile_version"
        ),
        @Index(
            name = "idx_dqa_profile",
            columnList = "profile_name, profile_version"
        ),
        @Index(
            name = "idx_dqa_score",
            columnList = "quality_score"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataQualityAssessment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id")
    private EntityRevision revision;

    @Column(name = "profile_name", nullable = false, length = 100)
    private String profileName;

    @Column(name = "profile_version", nullable = false, length = 30)
    private String profileVersion;

    /**
     * Version of the validator implementation.
     * Useful when rules change without profile changes.
     */
    @Column(name = "engine_version", nullable = false, length = 100)
    private String engineVersion;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @Column(name = "valid", nullable = false)
    private Boolean valid;

    @Column(name = "publication_candidate", nullable = false)
    private Boolean publicationCandidate;

    @Column(name = "passed_rules", nullable = false)
    private Integer passedRules;

    @Column(name = "info_failed_rules", nullable = false)
    private Integer infoFailedRules;

    @Column(name = "warning_failed_rules", nullable = false)
    private Integer warningFailedRules;

    @Column(name = "error_failed_rules", nullable = false)
    private Integer errorFailedRules;

    @Column(name = "total_points", nullable = false)
    private Double totalPoints;

    @Column(name = "achieved_points_normalised", nullable = false)
    private Double achievedPointsNormalised;

    @Column(name = "quality_score", nullable = false)
    private Double qualityScore;

    @Column(name = "total_points_fair", nullable = false)
    private Double totalPointsFair;

    @Column(name = "achieved_fair_points_normalised", nullable = false)
    private Double achievedFairPointsNormalised;

    @Column(name = "quality_score_fair", nullable = false)
    private Double qualityScoreFair;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<ConstraintEvaluationResult> issues = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<QualityDimension, DimensionScore> dimensionScores =
        new EnumMap<>(QualityDimension.class);
}
