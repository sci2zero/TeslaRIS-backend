package rs.teslaris.migrator.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.repository.MigrationRecordLogRepository;
import rs.teslaris.migrator.util.MigrationEntityType;

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
}
