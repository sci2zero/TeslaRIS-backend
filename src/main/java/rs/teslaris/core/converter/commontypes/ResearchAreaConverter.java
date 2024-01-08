package rs.teslaris.core.converter.commontypes;

import java.util.Objects;
import rs.teslaris.core.dto.commontypes.ResearchAreaDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;

public class ResearchAreaConverter {
    public static ResearchAreaDTO toDTO(ResearchArea researchArea) {
        var dto = new ResearchAreaDTO();

        dto.setName(MultilingualContentConverter.getMultilingualContentDTO(researchArea.getName()));
        dto.setDescription(
            MultilingualContentConverter.getMultilingualContentDTO(researchArea.getDescription()));

        if (Objects.nonNull(researchArea.getSuperResearchArea())) {
            dto.setSuperResearchArea(
                ResearchAreaConverter.toDTO(researchArea.getSuperResearchArea()));
        }

        return dto;
    }
}
