package rs.teslaris.importer.model.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;
import rs.teslaris.core.indexmodel.DocumentPublicationType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Document(collection = "documentImports")
public class DocumentImport {

    @MongoId(targetType = FieldType.OBJECT_ID)
    private String id;

    @Field("identifier")
    private String identifier;

    @Field("embedding")
    private List<Double> embedding;

    @Field("import_users_id")
    private Set<Integer> importUsersId = new HashSet<>();

    @Field("import_institutions_id")
    private Set<Integer> importInstitutionsId = new HashSet<>();

    @Field("is_loaded")
    private Boolean loaded = false;

    @Field("title")
    private List<MultilingualContent> title = new ArrayList<>();

    @Field("subtitle")
    private List<MultilingualContent> subtitle = new ArrayList<>();

    @Field("description")
    private List<MultilingualContent> description = new ArrayList<>();

    @Field("keywords")
    private List<MultilingualContent> keywords = new ArrayList<>();

    @Field("published_in")
    private List<MultilingualContent> publishedIn = new ArrayList<>();

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

    @Field("isbn")
    private String isbn;

    @Field("e_isbn")
    private String eisbn;

    @Field(name = "start_page")
    private String startPage;

    @Field(name = "end_page")
    private String endPage;

    @Field(name = "number_of_pages")
    private Integer numberOfPages;

    @Field(name = "article_number")
    private String articleNumber;

    @Field(name = "volume")
    private String volume;

    @Field(name = "issue")
    private String issue;

    @Field(name = "journal_open_alex_id")
    private String journalOpenAlexId;

    @Field(name = "open_alex_id")
    private String openAlexId;

    @Field(name = "wos_id")
    private String webOfScienceId;

    @Field(name = "internal_identifiers")
    private Set<String> internalIdentifiers = new HashSet<>();
}
