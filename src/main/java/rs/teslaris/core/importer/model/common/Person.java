package rs.teslaris.core.importer.model.common;

import jakarta.persistence.Column;
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

    @Column(name = "e_cris_id")
    private String eCrisId;

    @Column(name = "e_nauka_id")
    private String eNaukaId;

    @Field("orcid")
    private String orcid;

    @Field("scopus_id")
    private String scopusAuthorId;
}
