package rs.teslaris.core.exporter.model.common;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.Event;
import rs.teslaris.core.importer.model.common.MultilingualContent;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "publicationExports")
public class ExportPublication extends BaseExportEntity {

    @Field("type")
    private DocumentPublicationType type;

    @Field("title")
    private List<MultilingualContent> title;

    @Field("subtitle")
    private List<MultilingualContent> subtitle;

    @Field("description")
    private List<MultilingualContent> description;

    @Field("keywords")
    private List<MultilingualContent> keywords;

    @Field("document_date")
    private LocalDate documentDate;

    @Field("languages")
    private List<String> languageTags;

    @Field("number")
    private String number;

    @Field("volume")
    private String volume;

    @Field("issue")
    private String issue;

    @Field("edition")
    private String edition;

    @Field("start_page")
    private String startPage;

    @Field("end_page")
    private String endPage;

    @Field("uris")
    private List<String> uris;

    @Field("doi")
    private String doi;

    @Field("scopus_id")
    private String scopus;

    @Field("open_access")
    private Boolean openAccess;

    @Field("event")
    private Event event;

    @Field("proceedings")
    private ExportPublication proceedings;

    @Field("journal")
    private ExportPublicationSeries journal;

    @Field("authors")
    private List<ExportPerson> authors;

    @Field("editors")
    private List<ExportPerson> editors;
}
