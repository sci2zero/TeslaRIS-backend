package rs.teslaris.core.migration;

import jakarta.annotation.Nullable;
import java.util.Map;
import rs.teslaris.core.dto.project.ProjectDTO;

/**
 * Writes a migrated project, so that a module importing from an external source does not have to
 * depend on the project module. Implemented there, consumed by the importing module.
 */
public interface ProjectMigrationWriter {

    /**
     * Idempotent in the migrated record: passing the id a previous run produced updates that
     * project instead of creating a second one, so a re-run cannot duplicate. The caller is the
     * one holding that mapping, hence the parameter.
     *
     * @param project          the project to write
     * @param existingProjectId id a previous run produced for the same source record, or
     *                          {@code null} when this record has not been migrated yet
     */
    MigratedProject migrate(ProjectDTO project, @Nullable Integer existingProjectId);

    /**
     * @param id                           the written project
     * @param organisationContributionIds  contribution id per consortium member order number, which
     *                                     the project's fundings need to attach their parts to
     * @param created                      false when an existing project was updated instead
     */
    record MigratedProject(
        Integer id,
        Map<Integer, Integer> organisationContributionIds,
        boolean created
    ) {
    }
}
