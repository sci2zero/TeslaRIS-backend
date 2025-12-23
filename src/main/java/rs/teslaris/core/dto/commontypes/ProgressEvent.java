package rs.teslaris.core.dto.commontypes;

public record ProgressEvent(
    int percent,
    String stage
) {
}
