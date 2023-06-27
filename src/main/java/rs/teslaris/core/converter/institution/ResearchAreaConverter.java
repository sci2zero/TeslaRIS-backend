package rs.teslaris.core.converter.institution;

import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.dto.institution.ResearchAreaResponseDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;

public class ResearchAreaConverter {

    public static ResearchAreaResponseDTO toResponseDTO(ResearchArea researchArea) {
        var researchAreaResponse = new ResearchAreaResponseDTO();
        researchAreaResponse.setId(researchArea.getId());
        researchAreaResponse.setName(
            MultilingualContentConverter.getMultilingualContentDTO(
                researchArea.getName()));
        researchAreaResponse.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(
                researchArea.getDescription()));

        if (researchArea.getSuperResearchArea() != null) {
            researchAreaResponse.setSuperResearchAreaName(
                MultilingualContentConverter.getMultilingualContentDTO(
                    researchArea.getSuperResearchArea().getName()));
        } else {
            researchAreaResponse.setSuperResearchAreaName(null);
        }

        return researchAreaResponse;
    }
}
