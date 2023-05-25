package rs.teslaris.core.converter.institution;

import rs.teslaris.core.converter.commontypes.MultilingualContentToMultilingualContentDTO;
import rs.teslaris.core.dto.institution.ResearchAreaResponseDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;

public class ResearchAreaToResearchAreaDTO {

    public static ResearchAreaResponseDTO toResponseDTO(ResearchArea researchArea) {
        var researchAreaResponse = new ResearchAreaResponseDTO();
        researchAreaResponse.setId(researchArea.getId());
        researchAreaResponse.setName(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                researchArea.getName()));
        researchAreaResponse.setDescription(
            MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                researchArea.getDescription()));

        if (researchArea.getSuperResearchArea() != null) {
            researchAreaResponse.setSuperResearchAreaName(
                MultilingualContentToMultilingualContentDTO.getMultilingualContentDTO(
                    researchArea.getSuperResearchArea().getName()));
        } else {
            researchAreaResponse.setSuperResearchAreaName(null);
        }

        return researchAreaResponse;
    }
}
