package rs.teslaris.revisioner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DataQualityFixatorPipelineDTO(

    @NotBlank
    String pipelineName,

    @NotBlank
    String profileName,

    Integer version,

    @NotNull
    List<String> matchedRemarkKeys,

    List<String> explicitlySkippedRemarkKeys,

    @NotNull
    List<String> appliedFixators,

    @NotNull
    Boolean createVersionOnEveryStep
) {
}
