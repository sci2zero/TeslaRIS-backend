package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
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
@Document(collection = "eventExports")
public class ExportEvent extends BaseExportEntity {

    @Field("name")
    private List<ExportMultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private List<ExportMultilingualContent> nameAbbreviation = new ArrayList<>();

    @Field("description")
    private List<ExportMultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<ExportMultilingualContent> keywords = new ArrayList<>();

    @Field("serial_event")
    private Boolean serialEvent;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;

    @Field("conf_id")
    private String confId;

    @Field("state")
    private List<ExportMultilingualContent> state = new ArrayList<>();

    @Field("place")
    private List<ExportMultilingualContent> place = new ArrayList<>();
}
