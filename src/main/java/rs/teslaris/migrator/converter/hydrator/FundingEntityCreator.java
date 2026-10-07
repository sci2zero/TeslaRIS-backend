package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingDTO;
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

    private final MigrationLog migrationLog;


    @Override
    public Integer create(FundingMigrationDTO dto, boolean performIndex) {
        if (Objects.nonNull(dto.rejection())) {
            throw new InvalidSourceValueException(dto.rejection());
        }

        var key = dto.projectSourceKey() + "#funding#" + dto.sourceId();
        var funding = dto.funding();

        funding.setProjectId(idResolver
            .resolve(HydratorSource.NAME, MigrationEntityType.PROJECT, dto.projectSourceKey())
            .orElseThrow(() -> new MigrationException(String.format(
                "Project '%s' has not been migrated yet - run the project pass first.",
                dto.projectSourceKey()))));

        setFunder(key, dto);
        setCurrency(key, funding);

        var created = fundingService.createFunding(funding);

        return Objects.isNull(created) ? null : created.getId();
    }

    // MAP-009/038: linked by identifier, otherwise kept as display name
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
        dropped(key, "MAP-009",
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
            dropped(key, "MAP-008", "currency '" + amount.getCurrencyCode() +
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
