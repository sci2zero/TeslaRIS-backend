package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.migration.ProjectMigrationLookup;

/**
 * Finds the project a curriculum funding belongs to by its strong reference: FCT code against
 * {@code nationalId} (case-insensitive), QREN against {@code internalIdentifiers}. Uncached, so a
 * project created by an earlier curriculum in the same run is found as well.
 */
@Component
@RequiredArgsConstructor
public class HydratorProjectResolver {

    private final ProjectMigrationLookup projectMigrationLookup;


    // Several matches are not resolved: picking one could attach the funding to the wrong project
    public Match resolve(HydratorFundingReference reference) {
        List<Integer> projectIds = switch (reference.kind()) {
            case FCT -> projectMigrationLookup.findIdsByNationalId(reference.lookupValue());
            case QREN -> projectMigrationLookup.findIdsByInternalIdentifier(reference.lookupValue());
            case WEAK -> null;
        };

        if (Objects.isNull(projectIds)) {
            return Match.missing("weak reference, not looked up");
        }

        return switch (projectIds.size()) {
            case 0 -> Match.missing("no project with reference " + reference.lookupValue());
            case 1 -> new Match(projectIds.getFirst(), null);
            default -> Match.missing("ambiguous reference " + reference.lookupValue() +
                ": matches " + projectIds.size() + " projects");
        };
    }

    public record Match(Integer projectId, String reason) {

        static Match missing(String reason) {
            return new Match(null, reason);
        }

        public boolean found() {
            return Objects.nonNull(projectId);
        }
    }
}
