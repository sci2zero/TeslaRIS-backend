package rs.teslaris.project.indexmodel.funding;

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
@Document(indexName = "funding")
@Setting(settingPath = "/configuration/index-config.json")
public class FundingIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, name = "name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String nameSr;

    @Field(type = FieldType.Keyword, name = "name_sr_sortable", normalizer = "serbian_normalizer")
    private String nameSrSortable;

    @Field(type = FieldType.Text, name = "name_other", analyzer = "english", searchAnalyzer = "english")
    private String nameOther;

    @Field(type = FieldType.Keyword, name = "name_other_sortable", normalizer = "english_normalizer")
    private String nameOtherSortable;

    @Field(type = FieldType.Text, name = "funder_name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String funderNameSr;

    @Field(type = FieldType.Keyword, name = "funder_name_sr_sortable", normalizer = "serbian_normalizer")
    private String funderNameSrSortable;

    @Field(type = FieldType.Text, name = "funder_name_other", analyzer = "english", searchAnalyzer = "english")
    private String funderNameOther;

    @Field(type = FieldType.Keyword, name = "funder_name_other_sortable", normalizer = "english_normalizer")
    private String funderNameOtherSortable;

    @Field(type = FieldType.Integer, name = "funder_id", store = true)
    private Integer funderId;

    @Field(type = FieldType.Integer, name = "project_id", store = true)
    private Integer projectId;

    @Field(type = FieldType.Integer, name = "funding_call_id", store = true)
    private Integer fundingCallId;


    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Date, name = "date_from")
    private LocalDate dateFrom;

    @Field(type = FieldType.Date, name = "date_to")
    private LocalDate dateTo;

    /**
     * The funded project's contributors and their institutions, copied from the project index so a
     * funding can be counted under an institution it belongs to - a funding carries no contributor
     * of its own. Empty for a funding attached to an involvement rather than a project.
     * <p>
     * Written when the funding is indexed, so a change to the project's consortium only reaches
     * its fundings once they are reindexed.
     */
    @Field(type = FieldType.Integer, name = "person_ids")
    private List<Integer> personIds = new ArrayList<>();

    @Field(type = FieldType.Integer, name = "organisation_unit_ids")
    private List<Integer> organisationUnitIds = new ArrayList<>();
}
