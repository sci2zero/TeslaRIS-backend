package rs.teslaris.exporter.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "researchAreaExports")
public class ExportResearchArea extends BaseExportEntity {

    @Field("name")
    private List<ExportMultilingualContent> name = new ArrayList<>();

    @Field("description")
    private List<ExportMultilingualContent> description = new ArrayList<>();

    @Field("super_research_area_id")
    private Integer superResearchAreaId;

    // Free text, for example the name of a classification. Null when the area has no scheme.
    @Field("scheme_name")
    private String schemeName;
}
