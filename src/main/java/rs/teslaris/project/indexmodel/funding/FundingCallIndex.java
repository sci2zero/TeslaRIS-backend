package rs.teslaris.project.indexmodel.funding;

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
import rs.teslaris.project.model.funding.FundingType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "funding_call")
@Setting(settingPath = "/configuration/index-config.json")
public class FundingCallIndex {

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

    @Field(type = FieldType.Text, name = "program_name_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String programNameSr;

    @Field(type = FieldType.Keyword, name = "program_name_sr_sortable", normalizer = "serbian_normalizer")
    private String programNameSrSortable;

    @Field(type = FieldType.Text, name = "program_name_other", analyzer = "english", searchAnalyzer = "english")
    private String programNameOther;

    @Field(type = FieldType.Keyword, name = "program_name_other_sortable", normalizer = "english_normalizer")
    private String programNameOtherSortable;

    @Field(type = FieldType.Integer, name = "program_id", store = true)
    private Integer programId;

    @Field(type = FieldType.Integer, name = "funder_id", store = true)
    private Integer funderId;

    @Field(type = FieldType.Integer, name = "databaseId", store = true)
    private Integer databaseId;

    @Field(type = FieldType.Date, name = "date_from")
    private LocalDate dateFrom;

    @Field(type = FieldType.Date, name = "date_to")
    private LocalDate dateTo;

    @Field(type = FieldType.Double, name = "amount")
    private double amount;

    // Maybe swap with the currencyId?
    @Field(type = FieldType.Text, name = "currency_symbol")
    private String currencySymbol;

    @Field(type = FieldType.Keyword, name = "types")
    private List<FundingType> types;
}
