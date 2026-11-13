package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.person.InvolvementType;

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

    @Field("involvement_id")
    private Integer involvementId;

    @Field("involvement_type")
    private InvolvementType involvementType;

    // Name of an external institution. It is set only when employmentInstitution is null.
    @Field("display_organisation_unit")
    private List<ExportMultilingualContent> displayOrganisationUnit = new ArrayList<>();
}
