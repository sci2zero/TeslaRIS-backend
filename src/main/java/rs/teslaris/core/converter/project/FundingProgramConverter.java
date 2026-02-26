package rs.teslaris.core.converter.project;

import java.util.Objects;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.dto.project.FundingProgramDTO;
import rs.teslaris.core.model.project.FundingProgram;

public class FundingProgramConverter {

    public static FundingProgramDTO toDTO(FundingProgram fundingProgram) {
        var dto = new FundingProgramDTO();

        dto.setId(fundingProgram.getId());
        dto.setName(
            MultilingualContentConverter.getMultilingualContentDTO(fundingProgram.getName()));
        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            fundingProgram.getDescription()));
        dto.setObjectives(
            MultilingualContentConverter.getMultilingualContentDTO(fundingProgram.getObjectives()));
        dto.setNameAbbreviation(MultilingualContentConverter.getMultilingualContentDTO(
            fundingProgram.getNameAbbreviation()));
        dto.setKeywords(
            MultilingualContentConverter.getMultilingualContentDTO(fundingProgram.getKeywords()));

        fundingProgram.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());
            dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        dto.setMonetaryAmount(new MonetaryAmountDTO());
        if (Objects.nonNull(fundingProgram.getTotal())) {
            dto.getMonetaryAmount().setAmount(fundingProgram.getTotal().getAmount());
            dto.getMonetaryAmount().setCurrencyId(fundingProgram.getTotal().getCurrency().getId());
        }

        dto.setFundingTypes(fundingProgram.getTypes());
        dto.setUris(fundingProgram.getUris());
        dto.setProgramOpens(fundingProgram.getProgramOpens());
        dto.setProgramCloses(fundingProgram.getProgramCloses());

        dto.setFunderId(fundingProgram.getFunder().getId());
        dto.setFunderName(MultilingualContentConverter.getMultilingualContentDTO(
            fundingProgram.getFunder().getName()));

        dto.setOaMandated(fundingProgram.getOaMandated());
        dto.setOaMandateUrl(fundingProgram.getOaMandateUrl());

        fundingProgram.getProgramDocuments().forEach(
            fileItem -> dto.getFileItems().add(DocumentFileConverter.toDTO(fileItem)));

        return dto;
    }
}
