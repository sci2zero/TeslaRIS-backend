package rs.teslaris.core.exporter.model.common;

import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.importer.model.common.OrganisationUnit;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "organisationUnitExports")
public class ExportOrganisationUnit extends OrganisationUnit {

    @Id
    private String id;

    @Field("super_organisation_unit")
    private ExportOrganisationUnit superOU;

    @Field("last_updated")
    private LocalDateTime lastUpdated;
}