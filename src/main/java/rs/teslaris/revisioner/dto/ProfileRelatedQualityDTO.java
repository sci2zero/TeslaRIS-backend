package rs.teslaris.revisioner.dto;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.List;

public record ProfileRelatedQualityDTO(

    String profileName,

    String profileVersion,

    @Nullable
    Instant assessmentDate,

    List<RelatedQualityDTO> relatedQuality
) {
}
