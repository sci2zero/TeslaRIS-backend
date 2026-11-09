package rs.teslaris.exporter.model.common;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;
import rs.teslaris.core.model.project.PersonProjectContributionType;
import rs.teslaris.core.model.project.PersonProjectInvestigationRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportProjectPersonContribution extends ExportContribution {

    @Field("contribution_type")
    private PersonProjectContributionType contributionType;

    @Field("investigation_role")
    private PersonProjectInvestigationRole investigationRole;

    @Field("date_from")
    private LocalDate dateFrom;

    @Field("date_to")
    private LocalDate dateTo;
}
