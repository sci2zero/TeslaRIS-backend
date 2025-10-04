package rs.teslaris.core.indexmodel;

import jakarta.persistence.Id;
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
@Document(indexName = "book_series")
@Setting(settingPath = "/configuration/index-config.json")
public class BookSeriesIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "title_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Keyword, name = "title_sr_sortable", normalizer = "serbian_normalizer")
    private String titleSrSortable;

    @Field(type = FieldType.Text, store = true, name = "title_other", analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Keyword, name = "title_other_sortable", normalizer = "english_normalizer")
    private String titleOtherSortable;

    @Field(type = FieldType.Keyword, store = true, name = "e_issn")
    private String eISSN;

    @Field(type = FieldType.Keyword, store = true, name = "print_issn")
    private String printISSN;

    @Field(type = FieldType.Keyword, store = true, name = "open_alex_id")
    private String openAlexId;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;
}
