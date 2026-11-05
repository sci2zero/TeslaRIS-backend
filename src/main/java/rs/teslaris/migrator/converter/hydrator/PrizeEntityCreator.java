package rs.teslaris.migrator.converter.hydrator;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.converter.person.PrizeConverter;
import rs.teslaris.core.service.interfaces.person.PrizeService;
import rs.teslaris.migrator.model.MigrationRun;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.EntityUpdater;
import rs.teslaris.migrator.pipeline.merge.MergeContext;
import rs.teslaris.migrator.pipeline.merge.MigrationMergePolicy;
import rs.teslaris.migrator.service.impl.MigrationIdResolver;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * Resolves the person migrated earlier in the pass, then delegates to the core service. Adding a
 * prize creates no person revision.
 */
@Component
@RequiredArgsConstructor
public class PrizeEntityCreator
    implements EntityCreator<PrizeMigrationDTO>, EntityUpdater<PrizeMigrationDTO> {

    private final PrizeService prizeService;

    private final MigrationIdResolver idResolver;

    private final MigrationMergePolicy mergePolicy;


    @Override
    public Integer create(PrizeMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var personId = idResolver.require(HydratorSource.NAME, MigrationEntityType.PERSON,
            dto.personSourceKey());

        var created = prizeService.addPrize(personId, dto.prize());

        return Objects.isNull(created) ? null : created.getId();
    }

    @Override
    public void update(Integer targetId, PrizeMigrationDTO dto, MigrationRun run) {
        dto.throwIfRejected();

        var merged = mergePolicy.merge(dto.prize(),
            PrizeConverter.toDTO(prizeService.findOne(targetId)),
            new MergeContext(HydratorSource.NAME, MigrationEntityType.PERSON_PRIZE.name(),
                dto.sourceKey()));

        prizeService.updatePrize(targetId, merged);
    }
}
