package rs.teslaris.importer.model.common;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrganisationUnit {

    @Field("name")
    private List<MultilingualContent> name = new ArrayList<>();

    @Field("name_abbreviation")
    private String nameAbbreviation;

    @Field("scopus_afid")
    private String scopusAfid;

    @Field("import_id")
    private String importId;

    @Field("ror")
    private String ror;

    @Field("open_alex_id")
    private String openAlexId;
}
