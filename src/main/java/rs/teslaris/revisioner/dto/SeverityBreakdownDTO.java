package rs.teslaris.revisioner.dto;

import rs.teslaris.revisioner.util.dataquality.RepositoryEntityType;

/**
 * How many issues of each severity one entity type carries.
 * <p>
 * Activities are counted on the row of their own rather than on the row of the record raising
 * them, so the entity-type rows and the Activities row partition the repository's issues instead of
 * overlapping.
 */
public record SeverityBreakdownDTO(

    RepositoryEntityType entityType,

    long errorIssues,

    long warningIssues,

    long infoIssues,

    boolean supported
) {
    public static SeverityBreakdownDTO unsupported(RepositoryEntityType entityType) {
        return new SeverityBreakdownDTO(entityType, 0, 0, 0, false);
    }
}
