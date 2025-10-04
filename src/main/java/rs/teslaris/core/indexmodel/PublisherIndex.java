package rs.teslaris.core.indexmodel;

import jakarta.persistence.Id;
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
@Document(indexName = "publisher")
@Setting(settingPath = "/configuration/index-config.json")
public class PublisherIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Keyword, name = "name_sr_sortable", normalizer = "serbian_normalizer")
    private String nameSrSortable;

    @Field(type = FieldType.Text, store = true, name = "name_other", analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Keyword, name = "name_other_sortable", normalizer = "english_normalizer")
    private String nameOtherSortable;

    @Field(type = FieldType.Text, store = true, name = "place_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String placeSr;

    @Field(type = FieldType.Keyword, name = "place_sr_sortable", normalizer = "serbian_normalizer")
    private String placeSrSortable;

    @Field(type = FieldType.Text, store = true, name = "place_other", analyzer = "english", searchAnalyzer = "english")
    private String placeOther;

    @Field(type = FieldType.Keyword, name = "place_other_sortable", normalizer = "english_normalizer")
    private String placeOtherSortable;

    @Field(type = FieldType.Text, store = true, name = "state_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String stateSr;

    @Field(type = FieldType.Keyword, name = "state_sr_sortable", normalizer = "serbian_normalizer")
    private String stateSrSortable;

    @Field(type = FieldType.Text, store = true, name = "state_other", analyzer = "english", searchAnalyzer = "english")
    private String stateOther;

    @Field(type = FieldType.Keyword, name = "state_other_sortable", normalizer = "english_normalizer")
    private String stateOtherSortable;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;
}
