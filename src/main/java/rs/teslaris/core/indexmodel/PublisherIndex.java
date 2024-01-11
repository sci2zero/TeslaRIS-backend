package rs.teslaris.core.indexmodel;

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
@Document(indexName = "publisher")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class PublisherIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Keyword, store = true, name = "name_sr_sortable")
    private String nameSrSortable;

    @Field(type = FieldType.Text, store = true, name = "name_other", analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Keyword, store = true, name = "name_other_sortable")
    private String nameOtherSortable;

    @Field(type = FieldType.Text, store = true, name = "place_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String placeSr;

    @Field(type = FieldType.Keyword, store = true, name = "place_sr_sortable")
    private String placeSrSortable;

    @Field(type = FieldType.Text, store = true, name = "place_other", analyzer = "english", searchAnalyzer = "english")
    private String placeOther;

    @Field(type = FieldType.Keyword, store = true, name = "place_other_sortable")
    private String placeOtherSortable;

    @Field(type = FieldType.Text, store = true, name = "state_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String stateSr;

    @Field(type = FieldType.Keyword, store = true, name = "state_sr_sortable")
    private String stateSrSortable;

    @Field(type = FieldType.Text, store = true, name = "state_other", analyzer = "english", searchAnalyzer = "english")
    private String stateOther;

    @Field(type = FieldType.Keyword, store = true, name = "state_other_sortable")
    private String stateOtherSortable;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;
}
