package rs.teslaris.project.converter.funding;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingPart;

public class FundingPartConverter {

    public static FundingPartDTO toDTO(FundingPart fundingPart) {
        var dto = new FundingPartDTO();

        dto.setId(fundingPart.getId());

        if (Objects.nonNull(fundingPart.getFunding())) {
            dto.setFundingId(fundingPart.getFunding().getId());
        }
        if (Objects.nonNull(fundingPart.getProjectEvent())) {
            dto.setProjectEventId(fundingPart.getProjectEvent().getId());
        }
        if (Objects.nonNull(fundingPart.getFundingApplication())) {
            dto.setFundingApplicationId(fundingPart.getFundingApplication().getId());
        }
        if (Objects.nonNull(fundingPart.getProjectDocument())) {
            dto.setProjectDocumentId(fundingPart.getProjectDocument().getId());
        }
        if (Objects.nonNull(fundingPart.getPersonProjectContribution())) {
            dto.setPersonProjectContributionId(fundingPart.getPersonProjectContribution().getId());
        }
        if (Objects.nonNull(fundingPart.getOrganisationUnitProjectContribution())) {
            dto.setOrganisationUnitProjectContributionId(
                fundingPart.getOrganisationUnitProjectContribution().getId());
        }

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
