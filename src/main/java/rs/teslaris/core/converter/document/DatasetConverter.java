package rs.teslaris.core.converter.document;

import java.util.Objects;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.model.document.Dataset;

public class DatasetConverter extends DocumentPublicationConverter {

    public static DatasetDTO toDTO(Dataset dataset) {
        var datasetDTO = new DatasetDTO();

        setCommonFields(dataset, datasetDTO);

        datasetDTO.setInternalNumber(dataset.getInternalNumber());
        if (Objects.nonNull(dataset.getPublisher())) {
            datasetDTO.setPublisherId(dataset.getPublisher().getId());
        }

        return datasetDTO;
    }
}
