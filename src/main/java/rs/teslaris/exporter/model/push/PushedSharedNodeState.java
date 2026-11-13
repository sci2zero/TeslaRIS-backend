package rs.teslaris.exporter.model.push;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Triples of a node that more than one entity links to, for example an external
 * institution. The node is deleted when no PushedEntityState has its URI in dependencies.
 */
@Getter
@Setter
@NoArgsConstructor
@Document(collection = "pushedSharedNodeStates")
public class PushedSharedNodeState {

    @Id
    private String id;

    @Field("target")
    private String target;

    @Field("uri")
    private String uri;

    @Field("triples")
    private List<String> triples = new ArrayList<>();

    @Field("pushed_at")
    private Date pushedAt;


    public PushedSharedNodeState(String target, String uri) {
        this.target = target;
        this.uri = uri;
    }
}
