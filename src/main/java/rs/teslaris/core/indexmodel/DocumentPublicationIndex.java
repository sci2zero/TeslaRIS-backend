package rs.teslaris.core.indexmodel;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "document_publication")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class DocumentPublicationIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "title_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Text, name = "title_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Text, name = "description_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String descriptionSr;

    @Field(type = FieldType.Text, name = "description_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String descriptionOther;

    @Field(type = FieldType.Text, name = "keywords_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String keywordsSr;

    @Field(type = FieldType.Text, name = "keywords_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String keywordsOther;

    @Field(type = FieldType.Text, name = "full_text_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String fullTextSr;

    @Field(type = FieldType.Text, name = "full_text_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String fullTextOther;

    @Field(type = FieldType.Integer, name = "author_ids", store = true)
    private List<Integer> authorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "authorNames", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String authorNames = "";

    @Field(type = FieldType.Integer, name = "editor_ids", store = true)
    private List<Integer> editorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "editorNames", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String editorNames = "";

    @Field(type = FieldType.Integer, name = "reviewer_ids", store = true)
    private List<Integer> reviewerIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "reviewerNames", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String reviewerNames = "";

    @Field(type = FieldType.Integer, name = "advisor_ids", store = true)
    private List<Integer> advisorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "advisorNames", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String advisorNames = "";

    @Field(type = FieldType.Integer, name = "year", store = true)
    private Integer year;

    @Field(type = FieldType.Text, name = "type", store = true)
    private String type;

    @Field(type = FieldType.Integer, name = "journal_id", store = true)
    private Integer journalId;

    @Field(type = FieldType.Integer, name = "event_id", store = true)
    private Integer eventId;

    @Field(type = FieldType.Integer, name = "publisher_id", store = true)
    private Integer publisherId;

    @Field(type = FieldType.Integer, name = "database_id", store = true)
    private Integer databaseId;
}
