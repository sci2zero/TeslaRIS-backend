package rs.teslaris.revisioner.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import rs.teslaris.revisioner.dto.DataQualityFixatorPipelineDTO;
import rs.teslaris.revisioner.model.fixator.DataQualityFixatorPipeline;
import rs.teslaris.revisioner.util.fixators.DataQualityFixatorConfigurationLoader.FixatorPipeline;

public class DataQualityFixatorPipelineConverter {

    public static DataQualityFixatorPipelineDTO toDTO(DataQualityFixatorPipeline pipeline) {
        return new DataQualityFixatorPipelineDTO(
            pipeline.getPipelineName(),
            pipeline.getVersion(),
            new ArrayList<>(nullSafe(pipeline.getMatchedRemarkKeys())),
            new ArrayList<>(nullSafe(pipeline.getAppliedFixators())),
            Objects.requireNonNullElse(pipeline.getCreateVersionOnEveryStep(), false)
        );
    }

    public static FixatorPipeline toConfigurationEntry(DataQualityFixatorPipeline pipeline) {
        return new FixatorPipeline(
            pipeline.getPipelineName(),
            pipeline.getVersion(),
            new ArrayList<>(nullSafe(pipeline.getMatchedRemarkKeys())),
            new ArrayList<>(nullSafe(pipeline.getAppliedFixators())),
            Objects.requireNonNullElse(pipeline.getCreateVersionOnEveryStep(), false)
        );
    }

    private static List<String> nullSafe(List<String> values) {
        return Objects.isNull(values) ? List.of() : values;
    }
}
