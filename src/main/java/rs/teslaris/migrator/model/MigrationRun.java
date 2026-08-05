package rs.teslaris.migrator.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.migrator.util.MigrationEntityType;

@Document(collection = "migration_run")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MigrationRun {

    @Id
    private String id;

    @Field("source")
    private String source;

    @Field("entity_type")
    private MigrationEntityType entityType;

    @Field("status")
    private MigrationRunStatus status;

    @Field("started_at")
    private Instant startedAt;

    @Field("finished_at")
    private Instant finishedAt;

    @Field("current_page")
    private int currentPage;

    @Field("batch_size")
    private int batchSize;

    @Field("perform_index")
    private boolean performIndex;

    @Field("modified_after")
    private String modifiedAfter;

    @Field("records_read")
    private long recordsRead;

    @Field("items_created")
    private long itemsCreated;

    @Field("items_resolved")
    private long itemsResolved;

    @Field("items_skipped")
    private long itemsSkipped;

    @Field("items_failed")
    private long itemsFailed;

    @Field("batches_failed")
    private long batchesFailed;

    @Field("triggered_by_user_id")
    private Integer triggeredByUserId;
}
