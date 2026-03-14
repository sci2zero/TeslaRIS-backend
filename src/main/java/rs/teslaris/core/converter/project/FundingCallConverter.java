package rs.teslaris.core.converter.project;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.project.FundingCallDTO;
import rs.teslaris.core.model.project.FundingCall;

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

        dto.setMonetaryAmount(new MonetaryAmountDTO());
        if (Objects.nonNull(fundingCall.getAmount())) {
            dto.getMonetaryAmount().setAmount(fundingCall.getAmount().getAmount());
            dto.getMonetaryAmount().setCurrencyId(fundingCall.getAmount().getCurrency().getId());
        }

        dto.setFundingTypes(fundingCall.getTypes());
        dto.setUris(fundingCall.getUris());
        dto.setDateFrom(fundingCall.getDateFrom());
        dto.setDateTo(fundingCall.getDateTo());

        dto.setFundingProgramId(fundingCall.getFundingProgram().getId());
        dto.setFundingProgramName(MultilingualContentConverter.getMultilingualContentDTO(
            fundingCall.getFundingProgram().getName()));

        dto.setOaMandated(fundingCall.getOaMandated());
        dto.setOaMandateUrl(fundingCall.getOaMandateUrl());

        fundingCall.getCallDocuments().forEach(
            fileItem -> dto.getFileItems().add(DocumentFileConverter.toDTO(fileItem)));

        return dto;
    }
}
