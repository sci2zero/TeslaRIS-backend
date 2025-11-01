package rs.teslaris.reporting.dto;

public record CollaborationLink(
    String source,
    String target,
    Long value,
    Integer width
) {
}
