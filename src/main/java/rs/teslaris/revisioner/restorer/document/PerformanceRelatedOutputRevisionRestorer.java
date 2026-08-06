package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class PerformanceRelatedOutputRevisionRestorer
    implements RevisionRestorer<PerformanceRelatedOutputDTO> {

    private final PerformanceRelatedOutputService performanceRelatedOutputService;


    @Override
    public String entityType() {
        return DocumentPublicationType.PERFORMANCE_RELATED_OUTPUT.name();
    }

    @Override
    public Class<PerformanceRelatedOutputDTO> dtoClass() {
        return PerformanceRelatedOutputDTO.class;
    }

    @Override
    public void restore(Integer entityId, PerformanceRelatedOutputDTO dto) {
        performanceRelatedOutputService.editPerformanceRelatedOutput(entityId, dto);
    }
}
