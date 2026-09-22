package rs.teslaris.migrator.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Outcome of one migrated item. Holds no payload - this is bookkeeping, not a staging store.
 * <p>
 * It provides resume (skip what already succeeded), deduplication (the same institution or
 * co-authored paper appears in many curricula), the id map used to link dependent entities, and the
 * precise input for retrying failures.
 */
@Document(collection = "migration_record_log")
@CompoundIndex(name = "source_type_key_idx",
    def = "{'source': 1, 'entity_type': 1, 'source_key': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationRecordLog {

    @Id
    private String id;

    @Field("run_id")
    private String runId;

    @Field("source")
    private String source;

    @Field("entity_type")
    private MigrationEntityType entityType;

    @Field("source_key")
    private String sourceKey;

    @Field("status")
    private MigrationItemStatus status;

    @Field("target_entity_id")
    private Integer targetEntityId;

    @Field("reason")
    private String reason;

    @Field("processed_at")
    private Instant processedAt;
}
