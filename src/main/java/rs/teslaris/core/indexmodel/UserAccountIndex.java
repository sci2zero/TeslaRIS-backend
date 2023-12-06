package rs.teslaris.core.indexmodel;

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
public class UserAccountIndex {

    @Field(type = FieldType.Text, store = true, name = "name")
    private String fullName;

    @Field(type = FieldType.Text, store = true, name = "email")
    private String email;

    @Field(type = FieldType.Text, store = true, name = "org_unit_name_sr", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String organisationUnitNameSr;

    @Field(type = FieldType.Text, store = true, name = "org_unit_name_other", analyzer = "english", searchAnalyzer = "english")
    private String organisationUnitNameOther;

    @Field(type = FieldType.Integer, store = true, name = "user_role")
    private String userRole;

    @Field(type = FieldType.Integer, store = true, name = "databaseId", index = false)
    private Integer databaseId;
}
