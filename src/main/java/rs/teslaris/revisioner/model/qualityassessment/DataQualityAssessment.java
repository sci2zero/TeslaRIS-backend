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
import java.util.List;
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

    @Column(nullable = false, length = 100)
    private String profileName;

    @Column(nullable = false, length = 30)
    private String profileVersion;

    /**
     * Version of the validator implementation.
     * Useful when rules change without profile changes.
     */
    @Column(nullable = false, length = 100)
    private String engineVersion;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant finishedAt;

    @Column(nullable = false)
    private Double qualityScore;

    @Column(nullable = false)
    private Boolean valid;

    @Column(nullable = false)
    private Integer passedRules;

    @Column(nullable = false)
    private Integer warningRules;

    @Column(nullable = false)
    private Integer failedRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<DataQualityIssue> issues = new ArrayList<>();
}
