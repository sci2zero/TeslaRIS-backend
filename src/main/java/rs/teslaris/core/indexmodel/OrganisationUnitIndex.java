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
@Document(indexName = "organisation-unit")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class OrganisationUnitIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Text, store = true, name = "name_other", analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Text, store = true, name = "keywords_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String keywordsSr;

    @Field(type = FieldType.Text, store = true, name = "keywords_other", analyzer = "english", searchAnalyzer = "english")
    private String keywordsOther;

    @Field(type = FieldType.Text, store = true, name = "research_areas_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String researchAreasSr;

    @Field(type = FieldType.Text, store = true, name = "research_areas_other", analyzer = "english", searchAnalyzer = "english")
    private String researchAreasOther;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;
}
