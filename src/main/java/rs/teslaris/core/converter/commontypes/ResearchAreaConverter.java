package rs.teslaris.core.converter.commontypes;

import java.util.Objects;
import rs.teslaris.core.dto.commontypes.ResearchAreaHierarchyDTO;
import rs.teslaris.core.model.commontypes.ResearchArea;

public class ResearchAreaConverter {
    public static ResearchAreaHierarchyDTO toDTO(ResearchArea researchArea) {
        var dto = new ResearchAreaHierarchyDTO();

        dto.setId(researchArea.getId());
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
