package rs.teslaris.revisioner.dto;

import java.util.List;
import rs.teslaris.revisioner.model.qualityassessment.PolicyUploadMode;

public record PolicyUploadResultDTO(

    String profileName,

    String version,

    PolicyUploadMode mode,

    boolean versionCreated,

    List<String> warnings
) {
}
