package rs.teslaris.core.exporter.model.common;

import jakarta.persistence.Id;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseExportEntity {

    @Id
    private String id;

    @Field("database_id")
    private Integer databaseId;

    @Field("deleted")
    private Boolean deleted = false;

    @Field("last_updated")
    private Date lastUpdated;
}
