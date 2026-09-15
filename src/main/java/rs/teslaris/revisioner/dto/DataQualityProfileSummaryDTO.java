package rs.teslaris.revisioner.dto;

/**
 * A profile without its rules, for the profile pickers - the full profile carries every rule in
 * every language and costs a database lookup per translated string to build.
 */
public record DataQualityProfileSummaryDTO(

    String profileName,

    String version
) {
}
