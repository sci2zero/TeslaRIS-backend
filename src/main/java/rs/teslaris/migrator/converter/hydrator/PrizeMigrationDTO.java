package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.PrizeDTO;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * A prize is added to a person migrated earlier in the same pass; the person is resolved against
 * the record log at creation time. {@code sourceId} is the distinction id, unique only within its
 * curriculum.
 */
public record PrizeMigrationDTO(
    String personSourceKey,
    String sourceId,
    PrizeDTO prize,
    String rejection
) implements MigrationDTO {

    // Distinction ids are unique only within a curriculum, hence the composite key
    public static String keyOf(String curriculumId, String distinctionId) {
        return curriculumId + "#distinction#" + distinctionId;
    }

    @Override
    public String sourceKey() {
        return keyOf(personSourceKey, sourceId);
    }
}
