package rs.teslaris.revisioner.model;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
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
import rs.teslaris.core.util.restoration.DegradedReference;
import rs.teslaris.revisioner.model.qualityassessment.DataQualityAssessment;

@Entity
@Table(
    name = "entity_revision",
    indexes = {
        @Index(
            name = "idx_revision_entity_lookup",
            columnList = "entityType, entityId, revisionTimestamp"
        ),
        @Index(
            name = "idx_revision_hash",
            columnList = "contentHash"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityRevision extends BaseEntity {

    @Column(name = "major_version", nullable = false)
    private Integer majorVersion = 1;

    @Column(name = "minor_version", nullable = false)
    private Integer minorVersion = 0;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "revision_timestamp", nullable = false)
    private Instant revisionTimestamp;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @OneToMany(
        mappedBy = "revision",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<DataQualityAssessment> assessments = new ArrayList<>();

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "compressed_content", nullable = false)
    private byte[] compressedContent;

    /**
     * References that could not be satisfied while restoring an earlier state, e.g. a contributor
     * whose person record has since been deleted. Empty for every revision that is not the result of
     * a restore.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "restoration_warnings", columnDefinition = "jsonb")
    @Builder.Default
    private List<DegradedReference> restorationWarnings = new ArrayList<>();


    public void addAssessment(DataQualityAssessment assessment) {
        assessment.setRevision(this);
        assessments.add(assessment);
    }

    public void removeAssessment(DataQualityAssessment assessment) {
        assessments.remove(assessment);
        assessment.setRevision(null);
    }
}
