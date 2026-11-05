package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.dto.person.PersonNameDTO;
import rs.teslaris.core.dto.project.OrganisationUnitProjectContributionDTO;
import rs.teslaris.core.dto.project.PersonProjectContributionDTO;
import rs.teslaris.core.migration.FundingMigrationWriter;
import rs.teslaris.core.migration.ProjectMigrationSupplementer;
import rs.teslaris.core.migration.ProjectMigrationWriter;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.migrator.util.ResolvedToExistingException;

/**
 * A curriculum funding with a strong reference is first looked up among existing projects: a match
 * is supplemented and reported as resolved ({@link ResolvedToExistingException}), a reference
 * matching several projects skips the item. Otherwise the project is created with the owner,
 * investigators and consortium, and the funding under it. The record log keeps the funding id.
 */
@Component
@RequiredArgsConstructor
public class CvFundingEntityCreator implements EntityCreator<CvFundingMigrationDTO> {

    private final ProjectMigrationWriter projectMigrationWriter;

    private final FundingMigrationWriter fundingMigrationWriter;

    private final ProjectMigrationSupplementer projectSupplementer;

    private final CurrencyService currencyService;

    private final HydratorProjectResolver projectResolver;

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorPersonResolver personResolver;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationIdResolver idResolver;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(CvFundingMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var key = dto.sourceKey();
        var ownerId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());

        setFunder(key, dto);
        setCurrency(key, dto.funding());
        var team = team(key, dto, ownerId);

        if (Objects.nonNull(dto.reference())) {
            resolveExisting(key, dto, team);
        }

        var project = dto.project();
        project.setPersons(new ArrayList<>(team));
        project.setOrganisations(new ArrayList<>(dto.consortium().stream()
            .map(entry -> contribution(key, entry))
            .toList()));

        var created = projectMigrationWriter.migrate(project, null, Map.of());
        if (Objects.isNull(created)) {
            return null;
        }

        dto.funding().setProjectId(created.id());
        return fundingMigrationWriter.migrate(dto.funding(), null).id();
    }

    // Returns only when no project matches; the fundings of a matched project are not touched yet
    private void resolveExisting(String key, CvFundingMigrationDTO dto,
                                 List<PersonProjectContributionDTO> team) {
        var match = projectResolver.resolve(dto.reference());

        if (match.ambiguous()) {
            dropped(key, HydratorMappingRule.cv(5), match.reason());
            throw new MigrationException(match.reason() + ", item skipped");
        }

        if (!match.found()) {
            return;
        }

        projectSupplementer.supplement(match.projectId(), dto.project(), team)
            .forEach(reason -> dropped(key, HydratorMappingRule.cv(17), reason));

        throw new ResolvedToExistingException(null,
            "supplemented project " + match.projectId() + " found by " + dto.reference().kind() +
                " '" + dto.reference().lookupValue() + "', its fundings not supplemented");
    }

    // Owner first, then investigators; repeated persons are not added twice
    private List<PersonProjectContributionDTO> team(String key, CvFundingMigrationDTO dto,
                                                    Integer ownerId) {
        var team = new ArrayList<PersonProjectContributionDTO>();

        var owner = dto.owner();
        owner.setPersonId(ownerId);
        owner.setOrderNumber(1);
        team.add(owner);

        var personIds = new HashSet<Integer>(List.of(ownerId));
        var names = new HashSet<String>();
        var unlinked = new TreeMap<String, Integer>();

        dto.investigators().forEach(entry -> {
            var contribution = investigator(entry, unlinked);
            if (Objects.isNull(contribution.getPersonId()) && Objects.isNull(entry.name())) {
                return;
            }

            var duplicate = Objects.nonNull(contribution.getPersonId()) ?
                !personIds.add(contribution.getPersonId()) :
                !names.add(conversionUtil.normalise(entry.name()));
            if (duplicate) {
                return;
            }

            contribution.setOrderNumber(team.size() + 1);
            team.add(contribution);
        });

        unlinked.forEach((reason, count) -> dropped(key, HydratorMappingRule.cv(16),
            count + " investigator(s) " + reason));

        return team;
    }

    // CW-001-MAP-000016: linked by Ciência ID, otherwise name only
    private PersonProjectContributionDTO investigator(ProjectMigrationDTO.TeamEntry entry,
                                                      Map<String, Integer> unlinked) {
        var contribution = new PersonProjectContributionDTO();
        contribution.setContributionType(entry.type());
        contribution.setContributionDescription(new ArrayList<>());
        contribution.setDisplayAffiliationStatement(new ArrayList<>());

        if (Objects.nonNull(entry.name())) {
            var name = new PersonNameDTO();
            name.setLastname(entry.name());
            contribution.setPersonName(name);
        }

        if (Objects.nonNull(entry.cienciaId())) {
            var match = personResolver.resolve(entry.cienciaId(), null);
            if (match.found()) {
                contribution.setPersonId(match.personId());
                return contribution;
            }
            unlinked.merge("not linked, name only: " + match.reason(), 1, Integer::sum);
        }

        return contribution;
    }

    // CW-001-MAP-000013: linked by identifier, otherwise kept as display name
    private OrganisationUnitProjectContributionDTO contribution(
        String key, ProjectMigrationDTO.ConsortiumEntry entry) {
        var contribution = new OrganisationUnitProjectContributionDTO();
        contribution.setContributionType(entry.type());
        contribution.setOrderNumber(entry.orderNumber());

        var match = institutionResolver.resolve(entry.institution());
        if (match.found()) {
            contribution.setOrganisationUnitId(match.organisationUnitId());
        } else {
            contribution.setDisplayOrganisationUnit(new ArrayList<>(
                conversionUtil.multilingualContent(entry.institution().name(), null)));
            dropped(key, HydratorMappingRule.cv(13),
                "institution not linked, kept as display name: " + match.reason());
        }

        return contribution;
    }

    // CW-001-MAP-000002: an unlinked funder is the display funder only if no programme name took it
    private void setFunder(String key, CvFundingMigrationDTO dto) {
        if (Objects.isNull(dto.funder())) {
            return;
        }

        var match = institutionResolver.resolve(dto.funder());
        if (match.found()) {
            dto.funding().setFunderId(match.organisationUnitId());
            return;
        }

        if (dto.funding().getDisplayFunder().isEmpty()) {
            dto.funding().setDisplayFunder(new ArrayList<>(
                conversionUtil.multilingualContent(dto.funder().name(), null)));
            dropped(key, HydratorMappingRule.cv(2),
                "funder not linked, kept as display name: " + match.reason());
            return;
        }

        dropped(key, HydratorMappingRule.cv(2), "funder '" + dto.funder().name() +
            "' not linked and not kept, display funder is the programme name: " +
            match.reason());
    }

    private void setCurrency(String key, FundingDTO funding) {
        var amount = funding.getAmount();
        if (Objects.isNull(amount)) {
            return;
        }

        var currency = Objects.isNull(amount.getCurrencyCode()) ? null :
            currencyService.findCurrencyByCode(amount.getCurrencyCode());

        if (Objects.isNull(currency)) {
            funding.setAmount(null);
            dropped(key, HydratorMappingRule.cvWithoutRule(), "currency '" +
                amount.getCurrencyCode() + "' not in registry, amount dropped");
            return;
        }

        amount.setCurrencyId(currency.getId());
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.CV_FUNDING.name(), key,
            rule, reason);
    }
}
