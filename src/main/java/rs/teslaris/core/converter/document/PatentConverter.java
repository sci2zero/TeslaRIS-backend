package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.Patent;

public class PatentConverter extends DocumentPublicationConverter {

    public static PatentDTO toDTO(Patent patent) {
        var patentDTO = new PatentDTO();

        setCommonFields(patent, patentDTO);

        patentDTO.setNumber(patent.getNumber());
        if (Objects.nonNull(patent.getPublisher())) {
            patentDTO.setPublisherId(patent.getPublisher().getId());
        }

        return patentDTO;
    }
}
