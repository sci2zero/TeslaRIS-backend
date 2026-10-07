package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Component
@RequiredArgsConstructor
public class ProjectEntityCreator implements EntityCreator<ProjectMigrationDTO> {

    private final ProjectService projectService;

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(ProjectMigrationDTO dto, boolean performIndex) {
        if (Objects.nonNull(dto.rejection())) {
            throw new InvalidSourceValueException(dto.rejection());
        }

        dto.consortium().forEach(entry ->
            dto.project().getOrganisations().add(contribution(dto.sourceId(), entry)));

        var created = projectService.createProject(dto.project());

        return Objects.isNull(created) ? null : created.getId();
    }

    // MAP-022: linked by identifier, otherwise kept as display name
    private OrganisationUnitProjectContributionDTO contribution(
        String key, ProjectMigrationDTO.ConsortiumEntry entry) {
        var contribution = new OrganisationUnitProjectContributionDTO();
        contribution.setContributionType(entry.type());
        contribution.setOrderNumber(entry.orderNumber());

        var match = institutionResolver.resolve(entry.institution());
        if (match.found()) {
            contribution.setOrganisationUnitId(match.organisationUnitId());
        } else {
            var name = Objects.isNull(entry.institution()) ? null : entry.institution().name();
            contribution.setDisplayOrganisationUnit(
                new ArrayList<>(conversionUtil.multilingualContent(name, null)));
            migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(), key,
                "MAP-022", entry.type() + " not linked, kept as display name: " + match.reason());
        }

        return contribution;
    }
}
