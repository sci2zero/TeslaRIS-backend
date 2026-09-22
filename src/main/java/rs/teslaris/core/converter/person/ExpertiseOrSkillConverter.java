package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.person.ExpertiseOrSkillResponseDTO;
import rs.teslaris.core.model.person.ExpertiseOrSkill;

public class ExpertiseOrSkillConverter {

    public static ExpertiseOrSkillResponseDTO toDTO(ExpertiseOrSkill expertiseOrSkill) {
        var dto = new ExpertiseOrSkillResponseDTO();
        dto.setId(expertiseOrSkill.getId());
        dto.setName(
            MultilingualContentConverter.getMultilingualContentDTO(
                expertiseOrSkill.getName()));
        dto.setDescription(MultilingualContentConverter.getMultilingualContentDTO(
            expertiseOrSkill.getDescription()));
        dto.setKeywords(MultilingualContentConverter.getMultilingualContentDTO(
            expertiseOrSkill.getKeywords()));

        dto.setFavorite(expertiseOrSkill.getFavorite());

        expertiseOrSkill.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());
            dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        dto.setProofs(new ArrayList<>());
        expertiseOrSkill.getProofs()
            .forEach(proof ->
                dto.getProofs().add(DocumentFileConverter.toDTO(proof))
            );

        return dto;
    }
}
