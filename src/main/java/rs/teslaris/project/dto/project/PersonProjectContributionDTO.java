package rs.teslaris.project.dto.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.project.PersonProjectContributionType;
import rs.teslaris.project.model.project.PersonProjectInvestigationRole;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonProjectContributionDTO extends PersonContributionDTO {

    @NotNull(message = "You have to provide a contribution type.")
    private PersonProjectContributionType contributionType;

    @NotNull(message = "You have to provide an investigation role.")
    private PersonProjectInvestigationRole investigationRole;

    @Valid
    private List<MultilingualContentDTO> otherRoleDescription = new ArrayList<>();

    @Valid
    private List<FundingPartDTO> fundingParts = new ArrayList<>();
}
