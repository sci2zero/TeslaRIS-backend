package rs.teslaris.project.converter.funding;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.converter.person.PersonContributionConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.model.commontypes.ApproveStatus;
import rs.teslaris.project.dto.funding.FundingCallDTO;
import rs.teslaris.project.dto.funding.PersonFundingCallContributionDTO;
import rs.teslaris.project.model.funding.FundingCall;
import rs.teslaris.project.model.funding.PersonFundingCallContribution;

public class FundingCallConverter {

    public static FundingCallDTO toDTO(FundingCall fundingCall) {
        var dto = new FundingCallDTO();

        dto.setId(fundingCall.getId());
        dto.setName(
            MultilingualContentConverter.getMultilingualContentDTO(fundingCall.getName()));
        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            fundingCall.getDescription()));
        dto.setObjectives(
            MultilingualContentConverter.getMultilingualContentDTO(fundingCall.getObjectives()));
        dto.setNameAbbreviation(MultilingualContentConverter.getMultilingualContentDTO(
            fundingCall.getNameAbbreviation()));
        dto.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(fundingCall.getKeywords()));

        fundingCall.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());
            dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        if (Objects.nonNull(fundingCall.getAmount())) {
            dto.setMonetaryAmount(new MonetaryAmountDTO());
            dto.getMonetaryAmount().setAmount(fundingCall.getAmount().getAmount());
            dto.getMonetaryAmount().setCurrencyId(fundingCall.getAmount().getCurrency().getId());
            dto.getMonetaryAmount()
                .setCurrencyCode(fundingCall.getAmount().getCurrency().getCode());
            dto.getMonetaryAmount()
                .setCurrencySymbol(fundingCall.getAmount().getCurrency().getSymbol());
        }

        if (Objects.nonNull(fundingCall.getFunder())) {
            dto.setFunderId(fundingCall.getFunder().getId());
            dto.setFunderName(MultilingualContentConverter.getMultilingualContentDTO(
                fundingCall.getFunder().getName()));
        }

        dto.setFundingTypes(fundingCall.getTypes());
        dto.setUris(fundingCall.getUris());
        dto.setDateFrom(fundingCall.getDateFrom());
        dto.setDateTo(fundingCall.getDateTo());

        if (Objects.nonNull(fundingCall.getFundingProgram())) {
            dto.setFundingProgramId(fundingCall.getFundingProgram().getId());
            dto.setFundingProgramName(MultilingualContentConverter.getMultilingualContentDTO(
                fundingCall.getFundingProgram().getName()));
        }

        dto.setOaMandated(fundingCall.getOaMandated());
        dto.setOaMandateUrl(fundingCall.getOaMandateUrl());

        fundingCall.getCallDocuments().forEach(
            fileItem -> dto.getFileItems().add(DocumentFileConverter.toDTO(fileItem)));

        dto.setContributors(fundingCallContributionToDTO(fundingCall.getContributors()));

        return dto;
    }

    public static ArrayList<PersonFundingCallContributionDTO> fundingCallContributionToDTO(
        Set<PersonFundingCallContribution> contributions) {
        var contributionDTOs = new ArrayList<PersonFundingCallContributionDTO>();
        contributions.stream().filter(c -> c.getApproveStatus().equals(ApproveStatus.APPROVED))
            .forEach((c) -> {
                var contribution = new PersonFundingCallContributionDTO();
                PersonContributionConverter.setCommonFields(contribution, c);

                contribution.setContributionType(c.getContributionType());

                contributionDTOs.add(contribution);
            });
        return contributionDTOs;
    }
}
