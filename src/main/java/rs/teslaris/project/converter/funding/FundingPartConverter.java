package rs.teslaris.project.converter.funding;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingPart;

import java.util.Objects;

public class FundingPartConverter {

    public static FundingPartDTO toDTO(FundingPart fundingPart) {
        var dto = new FundingPartDTO();

        dto.setId(fundingPart.getId());
        dto.setFundingId(fundingPart.getFunding().getId());
        dto.setProjectEventId(fundingPart.getProjectEvent().getId());
        dto.setFundingApplicationId(fundingPart.getFundingApplication().getId());
        dto.setProjectDocumentId(fundingPart.getProjectDocument().getId());
        dto.setPersonProjectContributionId(fundingPart.getPersonProjectContribution().getId());
        dto.setOrganisationUnitProjectContributionId(
                fundingPart.getOrganisationUnitProjectContribution().getId());

        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
                fundingPart.getDescription()));

        dto.setAmount(new MonetaryAmountDTO());
        if (Objects.nonNull(fundingPart.getAmount())) {
            dto.getAmount().setAmount(fundingPart.getAmount().getAmount());
            dto.getAmount().setCurrencyId(fundingPart.getAmount().getCurrency().getId());
        }

        return dto;
    }
}
