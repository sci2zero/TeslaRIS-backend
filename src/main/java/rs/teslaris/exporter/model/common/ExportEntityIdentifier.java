package rs.teslaris.exporter.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportEntityIdentifier {

    @Field("code")
    private String code;

    @Field("value")
    private String value;

    @Field("url_template")
    private String urlTemplate;
}
