package rs.teslaris.core.applicationevent;

public record PublicationCandidacyChangedEvent(
    String entityType,
    Integer entityId,
    boolean publicationCandidate
) {
}
