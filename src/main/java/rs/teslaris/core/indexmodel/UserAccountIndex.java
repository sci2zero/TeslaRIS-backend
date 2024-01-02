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
@Document(indexName = "user_account")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class UserAccountIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "full_name")
    private String fullName;

    @Field(type = FieldType.Text, store = true, name = "email")
    private String email;

    @Field(type = FieldType.Keyword, store = true, name = "email_sortable")
    private String emailSortable;

    @Field(type = FieldType.Text, store = true, name = "org_unit_name_sr", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String organisationUnitNameSr;

    @Field(type = FieldType.Text, store = true, name = "org_unit_name_other", analyzer = "english", searchAnalyzer = "english")
    private String organisationUnitNameOther;

    @Field(type = FieldType.Keyword, store = true, name = "user_role")
    private String userRole;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;

    @Field(type = FieldType.Boolean, store = true, name = "active", index = false)
    private Boolean active;
}
