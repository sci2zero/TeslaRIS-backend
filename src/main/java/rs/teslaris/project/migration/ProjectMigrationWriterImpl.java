package rs.teslaris.project.migration;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
public class ProjectMigrationWriterImpl implements ProjectMigrationWriter {

    private final ProjectService projectService;

    private final IdentifierService identifierService;

    private final EntityIdentifierRepository entityIdentifierRepository;

    private final ProjectIdentifierRepository projectIdentifierRepository;


    @Override
    @Transactional
    public MigratedProject migrate(ProjectDTO project, @Nullable Integer existingProjectId,
                                   Map<String, String> otherIdentifiers) {
        if (Objects.isNull(existingProjectId)) {
            var created = projectService.createProject(project);
            if (Objects.isNull(created)) {
                return null;
            }

            // A new project holds no identifiers yet, so there is nothing to check against
            var dropped = setOtherIdentifiers(created, otherIdentifiers, false);
            return new MigratedProject(created.getId(), contributionIds(created), true, dropped);
        }

        // Re-running a pass must not create a second project for the same source record, and the
        // identifier uniqueness checks in createProject would reject it anyway.
        projectService.updateProject(existingProjectId, project);

        var updated = projectService.findOne(existingProjectId);
        var dropped = setOtherIdentifiers(updated, otherIdentifiers, true);
        return new MigratedProject(existingProjectId, contributionIds(updated), false, dropped);
    }

    // Not through ProjectIdentifierService: it throws on a taken value, which a re-run always hits
    private Map<String, String> setOtherIdentifiers(Project project,
                                                    Map<String, String> otherIdentifiers,
                                                    boolean checkExisting) {
        var dropped = new HashMap<String, String>();
        if (Objects.isNull(otherIdentifiers)) {
            return dropped;
        }

        otherIdentifiers.forEach((code, value) -> {
            if (!StringUtil.valueExists(value)) {
                return;
            }

            var identifier = identifierService.getIdentifierByCode(code);
            if (Objects.isNull(identifier)) {
                dropped.put(code, "no identifier is configured for code " + code);
                return;
            }

            if (checkExisting) {
                // Already attached by a previous run
                if (projectIdentifierRepository.existsByProjectIdAndIdentifierIdAndValue(
                    project.getId(), identifier.getId(), value)) {
                    return;
                }

                if (entityIdentifierRepository.existsByValueForType(value,
                    ProjectIdentifier.class, null)) {
                    dropped.put(code, "value " + value + " is already taken by another project");
                    return;
                }
            }

            var projectIdentifier = new ProjectIdentifier();
            projectIdentifier.setProject(project);
            projectIdentifier.setIdentifier(identifier);
            projectIdentifier.setValue(value);
            projectIdentifierRepository.save(projectIdentifier);
        });

        return dropped;
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
