package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.DatasetDTO;
import rs.teslaris.core.model.document.Dataset;

@Service
public interface DatasetService {

    Dataset findDatasetById(Integer datasetId);

    DatasetDTO readDatasetById(Integer datasetId);

    Dataset createDataset(DatasetDTO datasetDTO, Boolean index);

    void editDataset(Integer datasetId, DatasetDTO datasetDTO);

    void deleteDataset(Integer datasetId);

    void reindexDatasets();

    void indexDataset(Dataset dataset);
}
