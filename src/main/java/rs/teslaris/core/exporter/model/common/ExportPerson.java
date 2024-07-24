package rs.teslaris.core.exporter.model.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.Person;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportPerson extends Person {

    @Field("sex")
    private Sex sex;

    private List<String> electronicAddresses;

    private List<ExportOrganisationUnit> employmentInstitutions;
}
