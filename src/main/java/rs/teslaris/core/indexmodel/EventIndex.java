package rs.teslaris.core.indexmodel;

import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.ArrayList;
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
@Document(indexName = "events")
@Setting(settingPath = "/configuration/index-config.json")
public class EventIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "name_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Keyword, name = "name_sr_sortable")
    private String nameSrSortable;

    @Field(type = FieldType.Text, name = "name_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Keyword, name = "name_other_sortable")
    private String nameOtherSortable;

    @Field(type = FieldType.Text, name = "description_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String descriptionSr;

    @Field(type = FieldType.Text, name = "description_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String descriptionOther;

    @Field(type = FieldType.Text, name = "keywords_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String keywordsSr;

    @Field(type = FieldType.Text, name = "keywords_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String keywordsOther;

    @Field(type = FieldType.Text, name = "state_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String stateSr;

    @Field(type = FieldType.Keyword, name = "state_sr_sortable")
    private String stateSrSortable;

    @Field(type = FieldType.Text, name = "state_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String stateOther;

    @Field(type = FieldType.Keyword, name = "state_other_sortable")
    private String stateOtherSortable;

    @Field(type = FieldType.Text, name = "place_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String placeSr;

    @Field(type = FieldType.Text, name = "place_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String placeOther;

    @Field(type = FieldType.Text, name = "date_from_to", store = true)
    private String dateFromTo;

    @Field(type = FieldType.Date, name = "date_sortable")
    private LocalDate dateSortable;

    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Text, name = "event_type", store = true)
    private EventType eventType;

    @Field(type = FieldType.Boolean, name = "is_serial_event", store = true)
    private Boolean serialEvent;

    @Field(type = FieldType.Integer, name = "related_institution_ids", store = true)
    private List<Integer> relatedInstitutionIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "classified_by", store = true)
    private List<Integer> classifiedBy = new ArrayList<>();
}
