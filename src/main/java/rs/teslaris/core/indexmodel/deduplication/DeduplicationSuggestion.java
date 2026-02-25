package rs.teslaris.core.indexmodel.deduplication;

import jakarta.persistence.Id;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "deduplication_suggestion")
@Setting(settingPath = "/configuration/index-config.json")
public class DeduplicationSuggestion {

    @Id
    private String id;

    @Field(type = FieldType.Integer, store = true, name = "left_entity_database_id")
    private Integer leftEntityId;

    @Field(type = FieldType.Integer, store = true, name = "right_entity_database_id")
    private Integer rightEntityId;

    @Field(type = FieldType.Text, store = true, name = "left_title_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String leftTitleSr;

    @Field(type = FieldType.Text, store = true, name = "left_title_other", analyzer = "english", searchAnalyzer = "english")
    private String leftTitleOther;

    @Field(type = FieldType.Text, store = true, name = "right_title_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String rightTitleSr;

    @Field(type = FieldType.Text, store = true, name = "right_title_other", analyzer = "english", searchAnalyzer = "english")
    private String rightTitleOther;

    @Field(type = FieldType.Keyword, store = true, name = "document_type")
    private DocumentPublicationType documentPublicationType;

    @Field(type = FieldType.Keyword, store = true, name = "entity_type")
    private EntityType entityType;

    @Field(type = FieldType.Integer, store = true, name = "left_year")
    private Integer leftYear;

    @Field(type = FieldType.Integer, store = true, name = "right_year")
    private Integer rightYear;

    @Field(type = FieldType.Keyword, store = true, name = "left_authors")
    private String leftAuthors;

    @Field(type = FieldType.Keyword, store = true, name = "right_authors")
    private String rightAuthors;

    @Field(type = FieldType.Integer, store = true, name = "left_author_ids")
    private List<Integer> leftAuthorIds;

    @Field(type = FieldType.Integer, store = true, name = "right_author_ids")
    private List<Integer> rightAuthorIds;

    @Field(type = FieldType.Keyword, store = true, name = "left_concrete_type")
    private String leftConcreteType;

    @Field(type = FieldType.Keyword, store = true, name = "right_concrete_type")
    private String rightConcreteType;

    @Field(type = FieldType.Keyword, store = true, name = "publication_type")
    private String publicationType;


    public DeduplicationSuggestion(Integer leftEntityId, Integer rightEntityId, String leftTitleSr,
                                   String leftTitleOther, String rightTitleSr,
                                   String rightTitleOther,
                                   DocumentPublicationType documentPublicationType,
                                   EntityType entityType, Integer leftYear, Integer rightYear,
                                   String leftAuthors, String rightAuthors,
                                   List<Integer> leftAuthorIds,
                                   List<Integer> rightAuthorIds, String leftConcreteType,
                                   String rightConcreteType, String publicationType) {
        this.leftEntityId = leftEntityId;
        this.rightEntityId = rightEntityId;
        this.leftTitleSr = leftTitleSr;
        this.leftTitleOther = leftTitleOther;
        this.rightTitleSr = rightTitleSr;
        this.rightTitleOther = rightTitleOther;
        this.documentPublicationType = documentPublicationType;
        this.entityType = entityType;
        this.leftYear = leftYear;
        this.rightYear = rightYear;
        this.leftAuthors = leftAuthors;
        this.rightAuthors = rightAuthors;
        this.leftAuthorIds = leftAuthorIds;
        this.rightAuthorIds = rightAuthorIds;
        this.leftConcreteType = leftConcreteType;
        this.rightConcreteType = rightConcreteType;
        this.publicationType = publicationType;
    }
}
