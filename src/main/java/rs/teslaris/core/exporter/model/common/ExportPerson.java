package rs.teslaris.core.exporter.model.common;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.Person;
import rs.teslaris.core.model.person.Sex;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "personExports")
public class ExportPerson extends Person {

    @Id
    private String id;

    @Field("sex")
    private Sex sex;

    @Field("last_updated")
    private LocalDateTime lastUpdated;

    @Field("electronic_addresses")
    private List<String> electronicAddresses;

    @Field("employment_institutions")
    private List<ExportOrganisationUnit> employmentInstitutions;
}
