package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.InvalidSourceValueException;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

/**
 * Resolves the person migrated earlier in the pass, then delegates to the core service. Adding a
 * prize creates no person revision; indexing follows the run's {@code performIndex}.
 */
@Component
@RequiredArgsConstructor
public class PrizeEntityCreator implements EntityCreator<PrizeMigrationDTO> {

    private final PrizeService prizeService;

    private final MigrationIdResolver idResolver;


    @Override
    public Integer create(PrizeMigrationDTO dto, boolean performIndex) {
        if (Objects.nonNull(dto.rejection())) {
            throw new InvalidSourceValueException(dto.rejection());
        }

        var personId = idResolver
            .resolve(HydratorSource.NAME, MigrationEntityType.PERSON, dto.personSourceKey())
            .orElseThrow(() -> new MigrationException(String.format(
                "Person '%s' has not been migrated yet - run the person pass first.",
                dto.personSourceKey())));

        var created = prizeService.addPrize(personId, dto.prize(), performIndex);

        return Objects.isNull(created) ? null : created.getId();
    }
}
