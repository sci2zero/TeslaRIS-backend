package rs.teslaris.core.applicationevent;

public record DocumentClaimedEvent(
    Integer documentId,
    Integer personId,
    Integer authorIndex
) {
}
