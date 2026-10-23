package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Resolves the person migrated earlier in the pass, then delegates to the core service. Adding a
 * prize creates no person revision.
 */
@Component
@RequiredArgsConstructor
public class PrizeEntityCreator implements EntityCreator<PrizeMigrationDTO> {

    private final PrizeService prizeService;

    private final MigrationIdResolver idResolver;


    @Override
    public Integer create(PrizeMigrationDTO dto, boolean performIndex) {
        dto.throwIfRejected();

        var personId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());

        var created = prizeService.addPrize(personId, dto.prize());

        return Objects.isNull(created) ? null : created.getId();
    }
}
