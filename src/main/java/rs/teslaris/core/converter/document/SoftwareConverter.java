package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.model.document.Software;

public class SoftwareConverter extends DocumentPublicationConverter {

    public static SoftwareDTO toDTO(Software software) {
        var softwareDTO = new SoftwareDTO();

        setCommonFields(software, softwareDTO);

        softwareDTO.setInternalNumber(software.getInternalNumber());
        if (Objects.nonNull(software.getPublisher())) {
            softwareDTO.setPublisherId(software.getPublisher().getId());
        }

        return softwareDTO;
    }
}
