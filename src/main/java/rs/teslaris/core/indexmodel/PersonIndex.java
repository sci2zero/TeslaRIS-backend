package rs.teslaris.core.indexmodel;

import java.util.List;
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
@Document(indexName = "person")
public class PersonIndex {

    @Id
    private String id;

    @Field(type = FieldType.Text, store = true, name = "name")
    private String name;

    @Field(type = FieldType.Text, store = true, name = "employments")
    private String employments;

    @Field(name = "employmentInstitutionsId")
    private List<Integer> employmentInstitutionsId;

    @Field(type = FieldType.Text, store = true, name = "birthdate")
    private String birthdate;

    @Field(type = FieldType.Integer, store = true, name = "databaseId")
    private Integer databaseId;

}
