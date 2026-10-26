package rs.teslaris.revisioner.service.interfaces;

import java.util.List;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.dto.FixatorSummaryDTO;

public interface DataQualityFixatorService {

    List<FixatorSummaryDTO> listFixators();

    List<DataQualityFixatorPipelineDTO> listPipelines();

    DataQualityFixatorPipelineDTO createPipeline(DataQualityFixatorPipelineDTO pipeline);

    DataQualityFixatorPipelineDTO updatePipeline(String pipelineName,
                                                 DataQualityFixatorPipelineDTO pipeline);

    void deletePipeline(String pipelineName);

    void synchroniseWithConfiguration();
}
