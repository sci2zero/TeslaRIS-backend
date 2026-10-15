package rs.teslaris.migrator.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationException;

/**
 * Resolves the TeslaRIS id of something migrated earlier, by its source key.
 * <p>
 * The core {@code oldId} columns are numeric, while hydrator identifiers are Mongo string ids, so
 * the record log doubles as the id map for cross-entity references (a person's employments, a
 * document's contributions).
 */
@Component
@RequiredArgsConstructor
public class MigrationIdResolver {

    private final MigrationRecordLogRepository recordLogRepository;


    public Optional<Integer> resolve(String source, MigrationEntityType entityType,
                                     String sourceKey) {
        if (sourceKey == null || sourceKey.isBlank()) {
            return Optional.empty();
        }

        return recordLogRepository.findTargetEntityId(source, entityType, sourceKey);
    }

    /**
     * For references to an entity an earlier pass must have created.
     *
     * @throws MigrationException when that pass has not migrated it yet
     */
    public Integer require(String source, MigrationEntityType entityType, String sourceKey) {
        return resolve(source, entityType, sourceKey)
            .orElseThrow(() -> new MigrationException(String.format(
                "%s '%s' has not been migrated yet - run the %s pass first.",
                entityType, sourceKey, entityType)));
    }
}
