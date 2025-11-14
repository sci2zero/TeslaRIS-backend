package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportEmployment {

    @Field("employment_institution")
    private ExportOrganisationUnit employmentInstitution;

    @Field("from")
    private LocalDate dateFrom;

    @Field("to")
    private LocalDate dateTo;

    @Field("role")
    private String role;
}
