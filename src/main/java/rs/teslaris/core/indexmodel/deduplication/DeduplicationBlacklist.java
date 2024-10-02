package rs.teslaris.core.indexmodel.deduplication;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import rs.teslaris.core.indexmodel.IndexType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "deduplication_blacklist")
public class DeduplicationBlacklist {

    @Id
    private String id;

    @Field(type = FieldType.Integer, store = true, name = "left_entity_database_id")
    private Integer leftEntityId;

    @Field(type = FieldType.Integer, store = true, name = "right_entity_database_id")
    private Integer rightEntityId;

    @Field(type = FieldType.Keyword, store = true, name = "entity_type")
    private IndexType entityType;


    public DeduplicationBlacklist(Integer leftEntityId, Integer rightEntityId,
                                  IndexType entityType) {
        this.leftEntityId = leftEntityId;
        this.rightEntityId = rightEntityId;
        this.entityType = entityType;
    }
}
