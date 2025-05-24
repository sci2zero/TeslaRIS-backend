package rs.teslaris.core.indexmodel;


import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
@Setting(settingPath = "/configuration/index-config.json")
public class DocumentPublicationIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "title_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Keyword, store = true, name = "title_sr_sortable")
    private String titleSrSortable;

    @Field(type = FieldType.Text, name = "title_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Keyword, store = true, name = "title_other_sortable")
    private String titleOtherSortable;

    @Field(type = FieldType.Text, name = "description_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String descriptionSr;

    @Field(type = FieldType.Text, name = "description_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String descriptionOther;

    @Field(type = FieldType.Keyword, name = "keywords_sr", store = true)
    private String keywordsSr;

    @Field(type = FieldType.Keyword, name = "keywords_other", store = true)
    private String keywordsOther;

    @Field(type = FieldType.Text, name = "full_text_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String fullTextSr;

    @Field(type = FieldType.Text, name = "full_text_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String fullTextOther;

    @Field(type = FieldType.Integer, name = "author_ids", store = true)
    private List<Integer> authorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "author_names", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String authorNames = "";

    @Field(type = FieldType.Keyword, name = "author_names_sortable", store = true)
    private String authorNamesSortable = "";

    @Field(type = FieldType.Integer, name = "editor_ids", store = true)
    private List<Integer> editorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "editor_names", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String editorNames = "";

    @Field(type = FieldType.Integer, name = "reviewer_ids", store = true)
    private List<Integer> reviewerIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "board_member_ids", store = true)
    private List<Integer> boardMemberIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "board_president_id", store = true)
    private Integer boardPresidentId;

    @Field(type = FieldType.Text, name = "reviewer_names", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String reviewerNames = "";

    @Field(type = FieldType.Text, name = "board_member_names", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String boardMemberNames = "";

    @Field(type = FieldType.Text, name = "board_president_name", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String boardPresidentName = "";

    @Field(type = FieldType.Integer, name = "advisor_ids", store = true)
    private List<Integer> advisorIds = new ArrayList<>();

    @Field(type = FieldType.Text, name = "advisor_names", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String advisorNames = "";

    @Field(type = FieldType.Integer, name = "year", store = true)
    private Integer year;

    @Field(type = FieldType.Keyword, name = "type", store = true)
    private String type;

    @Field(type = FieldType.Keyword, name = "publication_type", store = true)
    private String publicationType;

    @Field(type = FieldType.Integer, name = "publication_series_id", store = true)
    private Integer publicationSeriesId;

    @Field(type = FieldType.Integer, name = "event_id", store = true)
    private Integer eventId;

    @Field(type = FieldType.Integer, name = "proceedings_id", store = true)
    private Integer proceedingsId;

    @Field(type = FieldType.Integer, name = "publisher_id", store = true)
    private Integer publisherId;

    @Field(type = FieldType.Integer, name = "journal_id", store = true)
    private Integer journalId;

    @Field(type = FieldType.Integer, name = "monograph_id", store = true)
    private Integer monographId;

    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Keyword, name = "doi", store = true)
    private String doi;

    @Field(type = FieldType.Keyword, name = "scopus_id", store = true)
    private String scopusId;

    @Field(type = FieldType.Integer, name = "organisation_unit_ids", store = true)
    private List<Integer> organisationUnitIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "claimer_ids", store = true)
    private List<Integer> claimerIds = new ArrayList<>();

    @Field(type = FieldType.Date, name = "last_edited")
    private Date lastEdited;

    @Field(type = FieldType.Integer, name = "assessed_by", store = true)
    private List<Integer> assessedBy = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "research_outputs", store = true)
    private List<Integer> researchOutputIds = new ArrayList<>();

    @Field(type = FieldType.Date, name = "topic_acceptance_date", store = true)
    private LocalDate topicAcceptanceDate;

    @Field(type = FieldType.Date, name = "thesis_defence_date", store = true)
    private LocalDate thesisDefenceDate;

    @Field(type = FieldType.Date, name = "public_review_start_dates", store = true)
    private List<LocalDate> publicReviewStartDates = new ArrayList<>();

    @Field(type = FieldType.Boolean, name = "is_open_access", store = true)
    private Boolean isOpenAccess;

    @Field(type = FieldType.Keyword, name = "wordcloud_tokens_sr", normalizer = "serbian_normalizer")
    private List<String> wordcloudTokensSr = new ArrayList<>();

    @Field(type = FieldType.Keyword, name = "wordcloud_tokens_other", normalizer = "english_normalizer")
    private List<String> wordcloudTokensOther = new ArrayList<>();
}
