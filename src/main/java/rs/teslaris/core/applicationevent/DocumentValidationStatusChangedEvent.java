package rs.teslaris.core.applicationevent;

public record DocumentValidationStatusChangedEvent(
    Integer documentId,
    boolean metadataValid,
    boolean filesValid,
    boolean publiclyVisible
) {
}
