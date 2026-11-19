package rs.teslaris.project.migration;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.migration.FundingMigrationWriter;
import rs.teslaris.core.repository.identifier.EntityIdentifierRepository;
import rs.teslaris.core.service.interfaces.identifier.IdentifierService;
import rs.teslaris.core.util.search.StringUtil;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.model.identifier.FundingIdentifier;
import rs.teslaris.project.repository.identifier.FundingIdentifierRepository;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@Service
@RequiredArgsConstructor
public class FundingMigrationWriterImpl implements FundingMigrationWriter {

    private final FundingService fundingService;

    private final IdentifierService identifierService;

    private final EntityIdentifierRepository entityIdentifierRepository;

    private final FundingIdentifierRepository fundingIdentifierRepository;


    @Override
    public FundingDTO read(Integer fundingId) {
        return fundingService.readFunding(fundingId);
    }

    @Override
    @Transactional
    public MigratedFunding migrate(FundingDTO funding, @Nullable Integer existingFundingId,
                                   Map<String, String> otherIdentifiers) {
        if (Objects.isNull(existingFundingId)) {
            var created = fundingService.createFunding(funding);
            if (Objects.isNull(created)) {
                return null;
            }

            // A new funding holds no identifiers yet, so there is nothing to check against
            var dropped = setOtherIdentifiers(created, otherIdentifiers, false);
            return new MigratedFunding(created.getId(), true, dropped);
        }

        // Re-running a pass must not create a second funding for the same source record.
        fundingService.updateFunding(existingFundingId, funding);

        var updated = fundingService.findOne(existingFundingId);
        var dropped = setOtherIdentifiers(updated, otherIdentifiers, true);
        return new MigratedFunding(existingFundingId, false, dropped);
    }

    // Not through FundingIdentifierService: it throws on a taken value, which a re-run always hits
    private Map<String, String> setOtherIdentifiers(Funding funding,
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
                if (fundingIdentifierRepository.existsByFundingIdAndIdentifierIdAndValue(
                    funding.getId(), identifier.getId(), value)) {
                    return;
                }

                if (entityIdentifierRepository.existsByValueForType(value,
                    FundingIdentifier.class, null)) {
                    dropped.put(code, "value " + value + " is already taken by another funding");
                    return;
                }
            }

            var fundingIdentifier = new FundingIdentifier();
            fundingIdentifier.setFunding(funding);
            fundingIdentifier.setIdentifier(identifier);
            fundingIdentifier.setValue(value);
            fundingIdentifierRepository.save(fundingIdentifier);
        });

        return dropped;
    }
}
