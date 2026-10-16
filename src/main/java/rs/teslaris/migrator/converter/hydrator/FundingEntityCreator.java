package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingDTO;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.service.interfaces.funding.FundingService;

/**
 * Resolves the project, the funder organisation unit and the currency, then creates the funding.
 */
@Component
@RequiredArgsConstructor
public class FundingEntityCreator implements EntityCreator<FundingMigrationDTO> {

    private final FundingService fundingService;

    private final CurrencyService currencyService;

    private final MigrationIdResolver idResolver;

    private final HydratorInstitutionResolver institutionResolver;

    private final HydratorConversionUtil conversionUtil;

    private final HydratorFundingPartFactory fundingPartFactory;

    private final MigrationLog migrationLog;


    @Override
    public Integer create(FundingMigrationDTO dto, boolean performIndex) {
        dto.throwIfRejected();

        var key = dto.sourceKey();
        var funding = dto.funding();

        funding.setProjectId(idResolver.require(HydratorSource.NAME, MigrationEntityType.PROJECT,
            dto.projectSourceKey()));

        setFunder(key, dto);
        setCurrency(key, funding);
        setFundingParts(key, dto);

        var created = fundingService.createFunding(funding);

        return Objects.isNull(created) ? null : created.getId();
    }

    // Consortium amounts of the project's only funding, each linked to its member's contribution
    private void setFundingParts(String key, FundingMigrationDTO dto) {
        var parts = new ArrayList<FundingPartDTO>();

        dto.consortiumAmounts().forEach(entry -> {
            var contributionId = idResolver.resolve(HydratorSource.NAME,
                MigrationEntityType.PROJECT_ORGANISATION,
                ProjectMigrationDTO.consortiumKeyOf(dto.projectSourceKey(), entry.orderNumber()));

            if (contributionId.isEmpty()) {
                dropped(key, HydratorMappingRule.sciproj(22), entry.type() + " #" +
                    entry.orderNumber() + " amount dropped: contribution not found");
                return;
            }

            fundingPartFactory.fundingPart(MigrationEntityType.PROJECT_FUNDING, key, entry)
                .ifPresent(part -> {
                    part.setOrganisationUnitProjectContributionId(contributionId.get());
                    parts.add(part);
                });
        });

        dto.funding().setFundingParts(parts);
    }

    // CW-002-MAP-000009/000038: linked by identifier, otherwise kept as display name
    private void setFunder(String key, FundingMigrationDTO dto) {
        if (Objects.isNull(dto.funder())) {
            return;
        }

        var match = institutionResolver.resolve(dto.funder());
        if (match.found()) {
            dto.funding().setFunderId(match.organisationUnitId());
            return;
        }

        dto.funding().setDisplayFunder(List.copyOf(
            conversionUtil.multilingualContent(dto.funder().name(), null)));
        dropped(key, HydratorMappingRule.sciproj(9),
            "funder not linked, kept as display name: " + match.reason());
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
            dropped(key, HydratorMappingRule.sciproj(8), "currency '" + amount.getCurrencyCode() +
                "' not in registry, amount dropped");
            return;
        }

        amount.setCurrencyId(currency.getId());
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, MigrationEntityType.PROJECT_FUNDING.name(),
            key, rule, reason);
    }
}
