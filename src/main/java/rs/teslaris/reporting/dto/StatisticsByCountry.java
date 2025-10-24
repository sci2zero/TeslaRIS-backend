package rs.teslaris.reporting.dto;

public record StatisticsByCountry(
    String countryCode,
    String countryName,
    long value
) {
}
