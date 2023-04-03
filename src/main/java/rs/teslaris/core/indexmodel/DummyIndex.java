package rs.teslaris.core.indexmodel;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "dummy")
public class DummyIndex {

    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("test_text")
    @Field(type = FieldType.Text, store = true, name = "test_text")
    private String testText;
}
