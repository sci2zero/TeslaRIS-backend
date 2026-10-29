package rs.teslaris.project.migration;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectMigrationWriterImpl implements ProjectMigrationWriter {

    private final ProjectService projectService;


    @Override
    @Transactional
    public MigratedProject migrate(ProjectDTO project, @Nullable Integer existingProjectId) {
        if (Objects.isNull(existingProjectId)) {
            var created = projectService.createProject(project);

            return Objects.isNull(created)
                ? null
                : new MigratedProject(created.getId(), contributionIds(created), true);
        }

        // Re-running a pass must not create a second project for the same source record, and the
        // identifier uniqueness checks in createProject would reject it anyway.
        projectService.updateProject(existingProjectId, project);

        return new MigratedProject(existingProjectId,
            contributionIds(projectService.findOne(existingProjectId)), false);
    }

    /**
     * Keyed by order number, which is what identifies a consortium member in the source - the
     * members have no identifier of their own.
     */
    private Map<Integer, Integer> contributionIds(Project project) {
        return project.getOrganisations().stream()
            .filter(contribution -> Objects.nonNull(contribution.getOrderNumber()))
            .collect(Collectors.toMap(
                OrganisationUnitProjectContribution::getOrderNumber,
                OrganisationUnitProjectContribution::getId,
                (first, second) -> first));
    }
}
