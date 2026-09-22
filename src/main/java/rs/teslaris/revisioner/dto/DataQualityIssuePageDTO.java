package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.util.List;

public record DataQualityIssuePageDTO(

    List<DataQualityIssueDTO> content,

    long totalIssues,

    @Nullable
    String nextCursor
) {
}
