package rs.teslaris.migrator.converter.hydrator;

import java.util.List;
import rs.teslaris.core.dto.person.involvement.EducationDTO;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.MigrationDTO;

/**
 * Person, organisation unit and thesis are resolved at creation time. The thesis is looked up
 * under each of {@code thesisSourceKeys} in turn.
 */
public record EducationMigrationDTO(
    String personSourceKey,
    String sourceKey,
    HydratorCVModel.Institution institution,
    EducationDTO education,
    List<String> thesisSourceKeys,
    String rejection
) implements MigrationDTO {
}
