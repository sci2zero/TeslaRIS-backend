package rs.teslaris.core.converter.document;

import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.model.document.Exhibition;

public class ExhibitionConverter extends EventConverter {

    public static ExhibitionDTO toDTO(Exhibition exhibition) {
        var dto = fillCommonFields(exhibition, new ExhibitionDTO());

        dto.setNumber(exhibition.getNumber());
        dto.setFee(exhibition.getFee());

        return dto;
    }
}
