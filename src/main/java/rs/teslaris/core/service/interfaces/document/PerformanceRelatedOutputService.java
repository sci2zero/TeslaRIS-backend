package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.model.document.PerformanceRelatedOutput;

@Service
public interface PerformanceRelatedOutputService {

    PerformanceRelatedOutput findPerformanceRelatedOutputById(Integer performanceRelatedOutputId);

    PerformanceRelatedOutputDTO readPerformanceRelatedOutputById(
        Integer performanceRelatedOutputId);

    PerformanceRelatedOutput createPerformanceRelatedOutput(
        PerformanceRelatedOutputDTO performanceRelatedOutputDTO, Boolean index);

    void editPerformanceRelatedOutput(Integer performanceRelatedOutputId,
                                      PerformanceRelatedOutputDTO performanceRelatedOutputDTO);

    void deletePerformanceRelatedOutput(Integer performanceRelatedOutputId);

    void reindexPerformanceRelatedOutputs();

    void indexPerformanceRelatedOutput(PerformanceRelatedOutput performanceRelatedOutput);

    PerformanceRelatedOutputDTO readPerformanceRelatedOutputByOldId(Integer oldId);
}
