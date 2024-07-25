package rs.teslaris.core.exporter.model.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportPublicationSeries {

    @Field("title")
    private List<ExportMultilingualContent> title;

    @Field("name_abbreviation")
    private List<ExportMultilingualContent> nameAbbreviation;

    @Field("e_issn")
    private String eIssn;

    @Field("print_issn")
    private String printIssn;

    // TODO: contributions contain only editors here?
    @Field("editors")
    private List<ExportPerson> editors;

    @Field("languages")
    private List<String> languageTags;
}
