package rs.teslaris.core.dto.document;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganisationUnitContributionDTO {

    private Integer id;

    private Integer organisationUnitId;

    private LocalDate dateFrom;

    private LocalDate dateTo;
}
