package rs.teslaris.core.exporter.model.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.MultilingualContent;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportPublisher {

    @Field("name")
    private List<MultilingualContent> name;
}