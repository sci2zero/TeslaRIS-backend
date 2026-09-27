package rs.teslaris.project.dto.project;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.project.model.project.OrganisationUnitProjectContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrepopulatedOrganisationDTO {

    private Integer organisationId;

    private List<MultilingualContentDTO> organisationName = new ArrayList<>();

    // TODO: Change to CountryDTO
    private String country;

    private MonetaryAmountDTO netContribution;

    private OrganisationUnitProjectContributionType contributionType;

    private String vatNumber; // this field is called taxNumber in the OrganisationUnit model
}