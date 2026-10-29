package rs.teslaris.project.migration;

import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.dto.funding.FundingDTO;
import rs.teslaris.core.migration.FundingMigrationWriter;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@Service
@RequiredArgsConstructor
public class FundingMigrationWriterImpl implements FundingMigrationWriter {

    private final FundingService fundingService;


    @Override
    @Transactional
    public MigratedFunding migrate(FundingDTO funding, @Nullable Integer existingFundingId) {
        if (Objects.isNull(existingFundingId)) {
            var created = fundingService.createFunding(funding);

            return Objects.isNull(created)
                ? null
                : new MigratedFunding(created.getId(), true);
        }

        // Re-running a pass must not create a second funding for the same source record.
        fundingService.updateFunding(existingFundingId, funding);

        return new MigratedFunding(existingFundingId, false);
    }
}
