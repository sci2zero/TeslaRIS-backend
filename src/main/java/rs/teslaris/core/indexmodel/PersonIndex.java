package rs.teslaris.core.indexmodel;

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
@Document(indexName = "person")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class PersonIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name")
    private String name;

    @Field(type = FieldType.Text, store = true, name = "employments_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String employmentsSr;

    @Field(type = FieldType.Text, store = true, name = "employments", analyzer = "english", searchAnalyzer = "english")
    private String employments;

    @Field(type = FieldType.Integer, name = "employmentInstitutionsId", store = true)
    private List<Integer> employmentInstitutionsId;

    @Field(type = FieldType.Text, store = true, name = "birthdate", index = false)
    private String birthdate;

    @Field(type = FieldType.Integer, store = true, name = "databaseId", index = false)
    private Integer databaseId;

}
