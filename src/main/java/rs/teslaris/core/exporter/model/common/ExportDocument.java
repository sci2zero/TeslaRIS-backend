package rs.teslaris.core.exporter.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.document.JournalPublicationType;
import rs.teslaris.core.model.document.MonographType;
import rs.teslaris.core.model.document.ProceedingsPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documentExports")
public class ExportDocument extends BaseExportEntity {

    @Field("type")
    private ExportPublicationType type;

    @Field("title")
    private List<ExportMultilingualContent> title = new ArrayList<>();

    @Field("name_abbreviation")
    private List<ExportMultilingualContent> nameAbbreviation = new ArrayList<>();

    @Field("subtitle")
    private List<ExportMultilingualContent> subtitle = new ArrayList<>();

    @Field("description")
    private List<ExportMultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<ExportMultilingualContent> keywords = new ArrayList<>();

    @Field("document_date")
    private String documentDate;

    @Field("languages")
    private List<String> languageTags = new ArrayList<>();

    @Field("number")
    private String number;

    @Field("volume")
    private String volume;

    @Field("issue")
    private String issue;

    @Field("start_page")
    private String startPage;

    @Field("end_page")
    private String endPage;

    @Field("uris")
    private List<String> uris = new ArrayList<>();

    @Field("doi")
    private String doi;

    @Field("scopus_id")
    private String scopus;

    @Field("e_isbn")
    private String eIsbn;

    @Field("print_isbn")
    private String printIsbn;

    @Field("e_issn")
    private String eIssn;

    @Field("print_issn")
    private String printIssn;

    @Field("open_access")
    private Boolean openAccess;

    @Field("event")
    private ExportEvent event;

    @Field("proceedings")
    private ExportDocument proceedings;

    @Field("edition")
    private String edition;

    @Field("journal")
    private ExportDocument journal;

    @Field("authors")
    private List<ExportContribution> authors = new ArrayList<>();

    @Field("editors")
    private List<ExportContribution> editors = new ArrayList<>();

    @Field("publishers")
    private List<ExportPublisher> publishers = new ArrayList<>();

    @Field("journal_publication_type")
    private JournalPublicationType journalPublicationType;

    @Field("proceedings_publication_type")
    private ProceedingsPublicationType proceedingsPublicationType;

    @Field("monograph_type")
    private MonographType monographType;
}
