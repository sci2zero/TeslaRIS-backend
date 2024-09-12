package rs.teslaris.core.indexmodel.deduplication;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.IndexType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "deduplication_suggestion")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class DeduplicationSuggestion {

    @Id
    private String id;

    @Field(type = FieldType.Integer, store = true, name = "left_entity_database_id")
    private Integer leftEntityId;

    @Field(type = FieldType.Integer, store = true, name = "right_entity_id")
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
    private IndexType entityType;


    public DeduplicationSuggestion(Integer leftEntityId, Integer rightEntityId, String leftTitleSr,
                                   String leftTitleOther, String rightTitleSr,
                                   String rightTitleOther,
                                   DocumentPublicationType documentPublicationType,
                                   IndexType entityType) {
        this.leftEntityId = leftEntityId;
        this.rightEntityId = rightEntityId;
        this.leftTitleSr = leftTitleSr;
        this.leftTitleOther = leftTitleOther;
        this.rightTitleSr = rightTitleSr;
        this.rightTitleOther = rightTitleOther;
        this.documentPublicationType = documentPublicationType;
        this.entityType = entityType;
    }
}
