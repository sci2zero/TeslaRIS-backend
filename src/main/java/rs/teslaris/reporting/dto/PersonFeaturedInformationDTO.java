package rs.teslaris.reporting.dto;

public record PersonFeaturedInformationDTO(
    Long publicationCount,
    Long currentCitationCount,
    Integer currentCitationTrend,
    Integer hIndex,
    Long journalPublicationsCount,
    Long proceedingsPublicationsCount,
    Long monographsCount,
    Long publicationsGain
) {
}
