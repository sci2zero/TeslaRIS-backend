package rs.teslaris.core.importer.model.common;

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

    @Field("mnid")
    private String mnid;

    @Field("orcid")
    private String orcid;

    @Field("scopus_id")
    private String scopusAuthorId;
}
