package rs.teslaris.core.indexmodel;

import jakarta.persistence.Id;
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
@Document(indexName = "journal")
@Setting(settingPath = "/configuration/index-config.json")
public class JournalIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "title_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Keyword, name = "title_sr_sortable")
    private String titleSrSortable;

    @Field(type = FieldType.Text, store = true, name = "title_other", analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Keyword, name = "title_other_sortable")
    private String titleOtherSortable;

    @Field(type = FieldType.Keyword, store = true, name = "e_issn")
    private String eISSN;

    @Field(type = FieldType.Keyword, store = true, name = "print_issn")
    private String printISSN;

    @Field(type = FieldType.Keyword, store = true, name = "open_alex_id")
    private String openAlexId;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;

    @Field(type = FieldType.Integer, name = "related_institution_ids", store = true)
    private List<Integer> relatedInstitutionIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "classified_by", store = true)
    private List<Integer> classifiedBy = new ArrayList<>();
}
