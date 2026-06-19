package rs.teslaris.project.indexmodel.project;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "project")
@Setting(settingPath = "/configuration/index-config.json")
public class ProjectIndex {

    @Id
    private String id;

    @JsonAlias("name_sr")
    @Field(type = FieldType.Text, name = "name_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Keyword, name = "name_sr_sortable", normalizer = "serbian_normalizer")
    private String nameSrSortable;

    @JsonAlias("name_other")
    @Field(type = FieldType.Text, name = "name_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Keyword, name = "name_other_sortable", normalizer = "english_normalizer")
    private String nameOtherSortable;

    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Date, name = "date_from")
    private LocalDate dateFrom;

    @Field(type = FieldType.Date, name = "date_to")
    private LocalDate dateTo;
}
