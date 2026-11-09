package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.project.OrganisationUnitProjectContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportProjectOrganisationContribution {

    @Field("organisation_unit")
    private ExportOrganisationUnit organisationUnit;

    @Field("display_name")
    private List<ExportMultilingualContent> displayName = new ArrayList<>();

    @Field("order_number")
    private Integer orderNumber;

    @Field("contribution_type")
    private OrganisationUnitProjectContributionType contributionType;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;
}
