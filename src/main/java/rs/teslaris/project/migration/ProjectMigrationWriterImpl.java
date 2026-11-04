package rs.teslaris.project.migration;

import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.project.ProjectDTO;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.repository.identifier.IdentifierRepository;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.model.identifier.ProjectIdentifier;
import rs.teslaris.project.model.project.OrganisationUnitProjectContribution;
import rs.teslaris.project.model.project.Project;
import rs.teslaris.project.repository.identifier.ProjectIdentifierRepository;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectMigrationWriterImpl implements ProjectMigrationWriter {

    private final ProjectService projectService;

    private final IdentifierRepository identifierRepository;

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

            setOtherIdentifiers(created, otherIdentifiers);
            return new MigratedProject(created.getId(), contributionIds(created), true);
        }

        // Re-running a pass must not create a second project for the same source record, and the
        // identifier uniqueness checks in createProject would reject it anyway.
        projectService.updateProject(existingProjectId, project);

        var updated = projectService.findOne(existingProjectId);
        setOtherIdentifiers(updated, otherIdentifiers);
        return new MigratedProject(existingProjectId, contributionIds(updated), false);
    }

    // Not through ProjectIdentifierService: it throws on a taken value, which a re-run always hits
    private void setOtherIdentifiers(Project project, Map<String, String> otherIdentifiers) {
        if (Objects.isNull(otherIdentifiers)) {
            return;
        }

        otherIdentifiers.forEach((code, value) -> {
            if (!StringUtil.valueExists(value)) {
                return;
            }

            var identifier = identifierRepository.findByCode(code);
            if (Objects.isNull(identifier)) {
                log.warn("No identifier is configured for code '{}', so value '{}' of project ({}) "
                    + "was skipped.", code, value, project.getId());
                return;
            }

            if (entityIdentifierRepository.existsByValueForType(value, ProjectIdentifier.class,
                null)) {
                return;
            }

            var projectIdentifier = new ProjectIdentifier();
            projectIdentifier.setProject(project);
            projectIdentifier.setIdentifier(identifier);
            projectIdentifier.setValue(value);
            projectIdentifierRepository.save(projectIdentifier);
        });
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
