package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.project.dto.project.PersonProjectContributionDTO;
import rs.teslaris.project.model.project.PersonProjectInvestigationRole;
import rs.teslaris.project.service.interfaces.project.ProjectService;

@Component
@RequiredArgsConstructor
public class ProjectEntityCreator implements EntityCreator<ProjectMigrationDTO> {

    private final ProjectService projectService;

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorPersonResolver personResolver;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(ProjectMigrationDTO dto, boolean performIndex) {
        if (Objects.nonNull(dto.rejection())) {
            throw new InvalidSourceValueException(dto.rejection());
        }

        dto.consortium().forEach(entry ->
            dto.project().getOrganisations().add(contribution(dto.sourceId(), entry)));

        // One log line per reason and project instead of one per team member
        var unlinked = new TreeMap<String, Integer>();
        dto.team().forEach(entry ->
            dto.project().getPersons().add(contribution(entry, unlinked)));
        unlinked.forEach((reason, count) ->
            migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(),
                dto.sourceId(), "MAP-028", count + " team member(s) " + reason));
        if (!dto.team().isEmpty()) {
            migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(),
                dto.sourceId(), "MAP-028",
                "investigationRole without source, provisional value: OTHER");
        }

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

    // MAP-028: linked by Ciência ID / ORCID, otherwise name only
    private PersonProjectContributionDTO contribution(ProjectMigrationDTO.TeamEntry entry,
                                                      Map<String, Integer> unlinked) {
        var contribution = new PersonProjectContributionDTO();
        contribution.setContributionType(entry.type());
        contribution.setInvestigationRole(PersonProjectInvestigationRole.OTHER);
        contribution.setOrderNumber(entry.orderNumber());
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());

        if (Objects.nonNull(entry.name())) {
            var name = new PersonNameDTO();
            name.setLastname(entry.name());
            contribution.setPersonName(name);
        }

        var match = personResolver.resolve(entry.cienciaId(), entry.orcid());
        if (match.found()) {
            contribution.setPersonId(match.personId());
        } else {
            unlinked.merge(entry.type() + " not linked, name only: " + match.reason(), 1,
                Integer::sum);
        }

        return contribution;
    }
}
