package rs.teslaris.project.dto.funding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.PersonContributionDTO;
import rs.teslaris.project.model.funding.FundingCallContributionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonFundingCallContributionDTO extends PersonContributionDTO {

    private FundingCallContributionType contributionType;
}
