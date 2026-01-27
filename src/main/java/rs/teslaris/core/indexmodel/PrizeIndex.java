package rs.teslaris.core.indexmodel;

import com.fasterxml.jackson.annotation.JsonAlias;
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
import rs.teslaris.core.util.functional.Triple;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "prize")
@Setting(settingPath = "/configuration/index-config.json")
public class PrizeIndex {

    @Id
    private String id;

    @JsonAlias("title_sr")
    @Field(type = FieldType.Text, name = "title_sr", store = true, analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Keyword, name = "title_sr_sortable", normalizer = "serbian_normalizer")
    private String titleSrSortable;

    @JsonAlias("title_other")
    @Field(type = FieldType.Text, name = "title_other", store = true, analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Keyword, name = "title_other_sortable", normalizer = "english_normalizer")
    private String titleOtherSortable;

    @Field(type = FieldType.Text, name = "description_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String descriptionSr;

    @Field(type = FieldType.Text, name = "description_other", analyzer = "english", searchAnalyzer = "english")
    private String descriptionOther;

    @Field(type = FieldType.Date, name = "date_of_acquisition")
    private LocalDate dateOfAcquisition;

    @Field(type = FieldType.Integer, name = "person_id", store = true)
    private Integer personId;

    @Field(type = FieldType.Text, name = "person_name", store = true, analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String personName;

    @Field(type = FieldType.Keyword, name = "person_name_sortable", store = true)
    private String personNameSortable;

    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Integer, name = "related_institutions_id_hierarchy", store = true)
    private List<Integer> relatedInstitutionsIdHierarchy = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "assessed_by", store = true)
    private List<Integer> assessedBy = new ArrayList<>();

    @Field(type = FieldType.Object, name = "commission_assessments")
    private List<Triple<Integer, String, Boolean>> commissionAssessments = new ArrayList<>();
}
