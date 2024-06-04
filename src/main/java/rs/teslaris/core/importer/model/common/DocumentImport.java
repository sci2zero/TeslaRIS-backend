package rs.teslaris.core.importer.model.common;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documentImports")
public class DocumentImport {

    @Id
    private String id;

    @Field("title")
    private List<MultilingualContent> title = new ArrayList<>();

    @Field("subtitle")
    private List<MultilingualContent> subtitle = new ArrayList<>();

    @Field("description")
    private List<MultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<MultilingualContent> keywords = new ArrayList<>();

    @Field("contributors")
    private List<PersonDocumentContribution> contributions = new ArrayList<>();

    @Field("uris")
    private List<String> uris = new ArrayList<>();

    @Field("document_date")
    private String documentDate;

    @Field("doi")
    private String doi;

    @Field("scopus_id")
    private String scopusId;

    @Field("event")
    private Event event;

    @Field("publication_type")
    private DocumentPublicationType publicationType;

    @Field("e_issn")
    private String eIssn;

    @Field("print_issn")
    private String printIssn;

    @Field(name = "start_page")
    private String startPage;

    @Field(name = "end_page")
    private String endPage;

    @Field(name = "number_of_pages")
    private Integer numberOfPages;
}
