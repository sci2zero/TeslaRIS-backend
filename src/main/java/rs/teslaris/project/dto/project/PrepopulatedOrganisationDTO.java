package rs.teslaris.project.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedOrganisationDTO {

    private Integer organisationId;

    // TODO: Add MLC and CountryDTO
    private String organisationName;

    private String country;

    private MonetaryAmountDTO netContribution;

    private OrganisationUnitProjectContributionType contributionType;
}