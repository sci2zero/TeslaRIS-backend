package rs.teslaris.assessment.dto.indicator;

public record ExternalIndicatorConfigurationDTO(
    Boolean showAltmetric,

    Boolean showDimensions,

    Boolean showOpenCitations,

    Boolean showPlumX,

    Boolean showUnpaywall
) {
}
