package rs.teslaris.core.indexmodel;

import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Date;
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
@Document(indexName = "person")
@Setting(settingPath = "/configuration/index-config.json")
public class PersonIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String name;

    @Field(type = FieldType.Text, store = true, name = "biography_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String biographySr;

    @Field(type = FieldType.Text, store = true, name = "biography_other", analyzer = "english", searchAnalyzer = "english")
    private String biographyOther;

    @Field(type = FieldType.Keyword, name = "name_sortable")
    private String nameSortable;

    @Field(type = FieldType.Text, store = true, name = "employments_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String employmentsSr;

    @Field(type = FieldType.Keyword, name = "employments_sr_sortable")
    private String employmentsSrSortable;

    @Field(type = FieldType.Text, store = true, name = "employments_other", analyzer = "english", searchAnalyzer = "english")
    private String employmentsOther;

    @Field(type = FieldType.Keyword, name = "employments_other_sortable")
    private String employmentsOtherSortable;

    @Field(type = FieldType.Integer, name = "employment_institutions_id", store = true)
    private List<Integer> employmentInstitutionsId = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "past_employment_institution_ids", store = true)
    private List<Integer> pastEmploymentInstitutionIds = new ArrayList<>();

    @Field(type = FieldType.Text, store = true, name = "birthdate")
    private String birthdate;

    @Field(type = FieldType.Keyword, name = "birthdate_sortable")
    private String birthdateSortable;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;

    @Field(type = FieldType.Keyword, store = true, name = "orcid")
    private String orcid;

    @Field(type = FieldType.Keyword, store = true, name = "scopus_author_id")
    private String scopusAuthorId;

    @Field(type = FieldType.Keyword, store = true, name = "open_alex_id")
    private String openAlexId;

    @Field(type = FieldType.Keyword, store = true, name = "web_of_science_researcher_id")
    private String webOfScienceResearcherId;

    @Field(type = FieldType.Text, store = true, name = "keywords")
    private String keywords;

    @Field(type = FieldType.Integer, store = true, name = "user_id")
    private Integer userId;

    @Field(type = FieldType.Integer, name = "employment_institutions_id_hierarchy", store = true)
    private List<Integer> employmentInstitutionsIdHierarchy = new ArrayList<>();

    @Field(type = FieldType.Date, name = "last_edited")
    private Date lastEdited;
}
