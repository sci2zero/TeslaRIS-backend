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
@Document(indexName = "document_file")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class DocumentFileIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "pdf_text_srp", analyzer = "serbian_analyzer", searchAnalyzer = "serbian_analyzer")
    private String pdfTextSrp;

    @Field(type = FieldType.Text, store = true, name = "pdf_text_other", analyzer = "english", searchAnalyzer = "english")
    private String pdfTextOther;

    @Field(type = FieldType.Text, store = true, name = "description_srp", analyzer = "serbian_analyzer", searchAnalyzer = "serbian_analyzer")
    private String descriptionSrp;

    @Field(type = FieldType.Text, store = true, name = "description_other", analyzer = "english", searchAnalyzer = "english")
    private String descriptionOther;

    @Field(type = FieldType.Text, store = true, name = "title_srp", analyzer = "serbian_analyzer", searchAnalyzer = "serbian_analyzer")
    private String titleSrp;

    @Field(type = FieldType.Text, store = true, name = "title_other", analyzer = "english", searchAnalyzer = "english")
    private String titleOther;

    @Field(type = FieldType.Text, store = true, name = "server_filename", index = false)
    private String serverFilename;
}
