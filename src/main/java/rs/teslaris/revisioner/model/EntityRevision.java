package rs.teslaris.revisioner.model;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(nullable = false, length = 100)
    private String entityType;

    @Column(nullable = false)
    private Integer entityId;

    @Column(nullable = false)
    private Instant revisionTimestamp;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] compressedContent;
}
