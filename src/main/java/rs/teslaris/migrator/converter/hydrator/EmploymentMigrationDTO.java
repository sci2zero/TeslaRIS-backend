package rs.teslaris.migrator.converter.hydrator;

import rs.teslaris.core.dto.person.involvement.EmploymentDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * Person and organisation unit are resolved at creation time.
 */
public record EmploymentMigrationDTO(
    String personSourceKey,
    String sourceId,
    HydratorCVModel.Institution institution,
    EmploymentDTO employment,
    String rejection
) implements MigrationDTO {

    // Employment ids are unique only within a curriculum, hence the composite key
    public static String keyOf(String curriculumId, String employmentId) {
        return curriculumId + "#employment#" + employmentId;
    }

    @Override
    public String sourceKey() {
        return keyOf(personSourceKey, sourceId);
    }
}
