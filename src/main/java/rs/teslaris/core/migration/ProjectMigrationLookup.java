package rs.teslaris.core.migration;

import java.util.List;

/**
 * Finds migrated projects by the identifiers an external source references them with, so that an
 * importing module does not have to depend on the project module. Implemented there.
 */
public interface ProjectMigrationLookup {

    /**
     * @return ids of projects with this national id, ignoring case, oldest first
     */
    List<Integer> findIdsByNationalId(String nationalId);

    /**
     * @return ids of projects holding this value among their internal identifiers, oldest first
     */
    List<Integer> findIdsByInternalIdentifier(String identifier);
}
