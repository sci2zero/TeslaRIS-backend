package rs.teslaris.importer.model.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Field("name")
    private PersonName name;

    @Field("apvnt")
    private String apvnt;

    @Field("e_cris_id")
    private String eCrisId;

    @Field("e_nauka_id")
    private String eNaukaId;

    @Field("orcid")
    private String orcid;

    @Field("scopus_id")
    private String scopusAuthorId;

    @Field("import_id")
    private String importId;
}
