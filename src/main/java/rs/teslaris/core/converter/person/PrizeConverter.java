package rs.teslaris.core.converter.person;

import java.util.ArrayList;
import java.util.HashSet;
import rs.teslaris.core.converter.commontypes.MultilingualContentConverter;
import rs.teslaris.core.converter.commontypes.ResearchAreaConverter;
import rs.teslaris.core.converter.document.DocumentFileConverter;
import rs.teslaris.core.dto.person.PrizeResponseDTO;
import rs.teslaris.core.model.person.Prize;

public class PrizeConverter {

    public static PrizeResponseDTO toDTO(Prize prize) {
        var dto = new PrizeResponseDTO(
            MultilingualContentConverter.getMultilingualContentDTO(prize.getTitle()),
            MultilingualContentConverter.getMultilingualContentDTO(prize.getDescription()),
            MultilingualContentConverter.getMultilingualContentDTO(prize.getKeywords()),
            prize.getDate(), prize.getEndDate(), prize.getType(),
            prize.getFavorite(), new HashSet<>(), prize.getId(), new ArrayList<>(),
            new ArrayList<>());

        prize.getResearchAreas().forEach(researchArea -> {
            dto.getResearchAreasId().add(researchArea.getId());
            dto.getResearchAreas().add(ResearchAreaConverter.toDTO(researchArea));
        });

        dto.setProofs(new ArrayList<>());
        prize.getProofs().forEach(proof -> {
            dto.getProofs().add(DocumentFileConverter.toDTO(proof));
        });

        return dto;
    }
}
