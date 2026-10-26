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

    @Field("orcid")
    private String orcid;

    @Field("scopus_id")
    private String scopusAuthorId;

    @Field("import_id")
    private String importId;

    @Field("open_alex_id")
    private String openAlexId;

    @Field("web_of_science_researcher_id")
    private String webOfScienceResearcherId;
}
