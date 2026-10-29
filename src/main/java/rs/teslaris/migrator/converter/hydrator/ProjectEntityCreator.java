package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

@Component
@RequiredArgsConstructor
public class ProjectEntityCreator implements EntityCreator<ProjectMigrationDTO> {

    private final ProjectMigrationWriter projectMigrationWriter;

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorPersonResolver personResolver;

    private final HydratorConversionUtil conversionUtil;

    private final HydratorFundingPartFactory fundingPartFactory;

    private final MigrationIdResolver idResolver;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(ProjectMigrationDTO dto, boolean performIndex) {
        dto.throwIfRejected();

        dto.consortium().forEach(entry ->
            dto.project().getOrganisations().add(contribution(dto.sourceId(), entry)));

        var amounts = dto.consortium().stream()
            .filter(entry -> Objects.nonNull(entry.amount()))
            .count();
        if (amounts > 0) {
            migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(),
                dto.sourceId(), HydratorMappingRule.sciproj(22), amounts +
                    " consortium amount(s) linked to contributions only: funding not determined");
        }

        // One log line per reason and project instead of one per team member
        var unlinked = new TreeMap<String, Integer>();
        dto.team().forEach(entry ->
            dto.project().getPersons().add(contribution(entry, unlinked)));
        unlinked.forEach((reason, count) ->
            migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT.name(),
                dto.sourceId(), HydratorMappingRule.sciproj(28),
                count + " team member(s) " + reason));

        // The id a previous run produced, so a re-run updates that project instead of creating a
        // second one for the same source record.
        var existingId = idResolver
            .resolve(HydratorSource.NAME, MigrationEntityType.PROJECT, dto.sourceId())
            .orElse(null);

        var migrated = projectMigrationWriter.migrate(dto.project(), existingId);
        if (Objects.isNull(migrated)) {
            return null;
        }

        // Fundings of the project link their parts to these contributions
        migrated.organisationContributionIds().forEach((orderNumber, contributionId) ->
            idResolver.remember(HydratorSource.NAME, MigrationEntityType.PROJECT_ORGANISATION,
                ProjectMigrationDTO.consortiumKeyOf(dto.sourceId(), orderNumber), contributionId));

        return migrated.id();
    }

    // CW-002-MAP-000022: linked by identifier, otherwise kept as display name
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
                HydratorMappingRule.sciproj(22),
                entry.type() + " not linked, kept as display name: " + match.reason());
        }

        // Several or no fundings: the part cannot be attributed to one of them
        if (Objects.nonNull(entry.amount())) {
            fundingPartFactory.fundingPart(MigrationEntityType.PROJECT, key, entry)
                .ifPresent(part -> contribution.setFundingParts(new ArrayList<>(List.of(part))));
        }

        return contribution;
    }

    // CW-002-MAP-000028: linked by Ciência ID / ORCID, otherwise name only
    private PersonProjectContributionDTO contribution(ProjectMigrationDTO.TeamEntry entry,
                                                      Map<String, Integer> unlinked) {
        var contribution = new PersonProjectContributionDTO();
        contribution.setContributionType(entry.type());
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
