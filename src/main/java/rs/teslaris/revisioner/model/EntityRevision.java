package rs.teslaris.revisioner.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import rs.teslaris.core.model.commontypes.BaseEntity;

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

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Integer entityId;

    @Column(name = "revision_timestamp", nullable = false)
    private Instant revisionTimestamp;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "quality_data_score", nullable = false)
    private Double qualityDataScore = 0.0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "uris")
    private Set<String> qualityDataReport = new HashSet<>();

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "compressed_content", nullable = false)
    private byte[] compressedContent;
}
