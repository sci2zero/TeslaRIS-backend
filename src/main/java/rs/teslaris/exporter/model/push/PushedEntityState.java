package rs.teslaris.exporter.model.push;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pushedEntityStates")
public class PushedEntityState {

    @Id
    private String id;

    @Field("target")
    private String target;

    @Field("entity_type")
    private PushEntityType entityType;

    @Field("database_id")
    private Integer databaseId;

    @Field("snapshot_last_updated")
    private Date snapshotLastUpdated;

    @Field("triples")
    private List<String> triples = new ArrayList<>();

    @Field("pushed_at")
    private Date pushedAt;

    // URIs of shared nodes that the triples link to.
    @Field("dependencies")
    private List<String> dependencies = new ArrayList<>();


    public PushedEntityState(String target, PushEntityType entityType, Integer databaseId) {
        this.target = target;
        this.entityType = entityType;
        this.databaseId = databaseId;
    }
}
