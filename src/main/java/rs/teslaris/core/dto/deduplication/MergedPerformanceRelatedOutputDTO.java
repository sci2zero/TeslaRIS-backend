package rs.teslaris.core.dto.deduplication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergedPerformanceRelatedOutputDTO extends MergedDocumentsDTO {

    private PerformanceRelatedOutputDTO leftPerformanceRelatedOutput;

    private PerformanceRelatedOutputDTO rightPerformanceRelatedOutput;
}
