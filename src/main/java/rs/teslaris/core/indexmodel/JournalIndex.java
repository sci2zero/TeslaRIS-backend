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
@Document(indexName = "journal")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class JournalIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "title_sr", analyzer = "serbian", searchAnalyzer = "serbian")
    private String titleSr;

    @Field(type = FieldType.Text, store = true, name = "title_other", analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Text, store = true, name = "e_issn")
    private String eISSN;

    @Field(type = FieldType.Text, store = true, name = "print_issn")
    private String printISSN;

    @Field(type = FieldType.Integer, store = true, name = "databaseId", index = false)
    private Integer databaseId;
}
