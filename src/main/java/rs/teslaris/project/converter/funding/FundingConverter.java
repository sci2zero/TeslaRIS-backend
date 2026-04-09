package rs.teslaris.project.converter.funding;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.model.funding.Funding;

import java.util.Objects;

public class FundingConverter {

    public static FundingDTO toDTO(Funding funding) {
        var dto = new FundingDTO();

        dto.setId(funding.getId());
        dto.setInternalIdentifiers(funding.getInternalIdentifiers());
        dto.setOldIds(funding.getOldIds());
        dto.setMergedIds(funding.getMergedIds());
        dto.setDoi(funding.getDoi());
        dto.setGrantAgreementId(funding.getGrantAgreementId());

        if (Objects.nonNull(funding.getProject())) {
            dto.setProjectId(funding.getProject().getId());
        }

        funding.getAgreements().forEach(
                fileItem -> dto.getAgreements().add(DocumentFileConverter.toDTO(fileItem)));

        dto.setName(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getName()));
        dto.setDescription(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getDescription()));
        dto.setNameAbbreviation(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getNameAbbreviation()));
        dto.setKeywords(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getKeywords()));

        funding.getResearchAreas().forEach(researchArea ->
                dto.getResearchAreasId().add(researchArea.getId()));

        dto.setUris(funding.getUris());

        if (Objects.nonNull(funding.getFundingCall())) {
            dto.setFundingCallId(funding.getFundingCall().getId());
        }

        if (Objects.nonNull(funding.getFunder())) {
            dto.setFunderId(funding.getFunder().getId());
        }

        dto.setDisplayCall(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getDisplayCall()));
        dto.setDisplayProgram(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getDisplayProgram()));
        dto.setDisplayFunder(
                MultilingualContentConverter.getMultilingualContentDTO(funding.getDisplayFunder()));

        dto.setFundingTypes(funding.getFundingTypes());

        dto.setAmount(new MonetaryAmountDTO());
        if (Objects.nonNull(funding.getAmount())) {
            dto.getAmount().setAmount(funding.getAmount().getAmount());
            dto.getAmount().setCurrencyId(funding.getAmount().getCurrency().getId());
        }

        dto.setCompetitive(funding.getCompetitive());
        dto.setRenewable(funding.getRenewable());
        dto.setDateSubmitted(funding.getDateSubmitted());
        dto.setDateAwarded(funding.getDateAwarded());
        dto.setDateFrom(funding.getDateFrom());
        dto.setDateTo(funding.getDateTo());
        dto.setOaMandated(funding.getOaMandated());
        dto.setOaMandateUrl(funding.getOaMandateUrl());

        return dto;
    }

}
