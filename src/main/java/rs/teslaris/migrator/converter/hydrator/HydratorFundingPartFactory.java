package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MonetaryAmountDTO;
import rs.teslaris.core.service.interfaces.commontypes.CurrencyService;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;
import rs.teslaris.project.dto.funding.FundingPartDTO;

/**
 * Funding part for a consortium member's amount (CW-002-MAP-000022), described by the member's
 * name as CORDIS imports do. Links to the funding and the contribution are left to the caller.
 */
@Component
@RequiredArgsConstructor
public class HydratorFundingPartFactory {

    private final CurrencyService currencyService;

    private final HydratorConversionUtil conversionUtil;

    private final MigrationLog migrationLog;


    public Optional<FundingPartDTO> fundingPart(MigrationEntityType entityType, String key,
                                                ProjectMigrationDTO.ConsortiumEntry entry) {
        var amount = entry.amount();
        var value = Objects.isNull(amount) ? null : parse(amount.value());

        if (Objects.isNull(value) || value <= 0) {
            dropped(entityType, key, entry,
                "invalid amount '" + (Objects.isNull(amount) ? null : amount.value()) + "'");
            return Optional.empty();
        }

        var currencyCode = Objects.isNull(amount.currency()) ? null :
            amount.currency().trim().toUpperCase();
        var currency =
            Objects.isNull(currencyCode) ? null : currencyService.findCurrencyByCode(currencyCode);

        if (Objects.isNull(currency)) {
            dropped(entityType, key, entry,
                "currency '" + amount.currency() + "' not in registry");
            return Optional.empty();
        }

        var part = new FundingPartDTO();
        part.setAmount(new MonetaryAmountDTO(currency, value));
        part.setDescription(conversionUtil.multilingualContent(
            Objects.isNull(entry.institution()) ? null : entry.institution().name(), null));

        return Optional.of(part);
    }

    private Double parse(String value) {
        if (Objects.isNull(value) || value.isBlank()) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void dropped(MigrationEntityType entityType, String key,
                         ProjectMigrationDTO.ConsortiumEntry entry, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME, entityType.name(), key,
            HydratorMappingRule.sciproj(22),
            entry.type() + " #" + entry.orderNumber() + " amount dropped: " + reason);
    }
}
