package rs.teslaris.core.exporter.model.common;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.Event;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "eventExports")
public class ExportEvent extends Event {

    @Id
    private String id;

    @Field("last_updated")
    private LocalDateTime lastUpdated;
}
