package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.RecordExtractor;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Curriculum {@code groups:fundings-outputs}: an output the owner marked as a result of one of
 * their fundings. The crosswalk has no rule for it; each pair becomes a project document of type
 * RESULT.
 */
@Component
@RequiredArgsConstructor
public class HydratorFundingOutputExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, ProjectDocumentMigrationDTO> {

    private final MigrationLog migrationLog;


    @Override
    public List<ProjectDocumentMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().groups()) ||
            Objects.isNull(record.curriculum().groups().fundingOutputs()) ||
            Objects.isNull(record.curriculum().groups().fundingOutputs().fundingOutput())) {
            return List.of();
        }

        var dtos = new ArrayList<ProjectDocumentMigrationDTO>();

        record.curriculum().groups().fundingOutputs().fundingOutput().forEach(pair -> {
            if (Objects.isNull(pair)) {
                return;
            }

            var fundingId = id(pair.funding());
            var outputId = id(pair.output());

            if (Objects.isNull(fundingId) || Objects.isNull(outputId)) {
                migrationLog.valueDropped(HydratorSource.NAME,
                    MigrationEntityType.PROJECT_DOCUMENT.name(), record.id(),
                    HydratorMappingRule.cvWithoutRule(), "funding-output pair without both ids");
                return;
            }

            dtos.add(new ProjectDocumentMigrationDTO(
                record.id() + "#funding-output#" + fundingId + "-" + outputId,
                CvFundingMigrationDTO.keyOf(record.id(), fundingId),
                record.id() + "#output#" + outputId,
                null));
        });

        return dtos;
    }

    private String id(HydratorCVModel.GroupReference reference) {
        if (Objects.isNull(reference) || Objects.isNull(reference.id()) ||
            reference.id().isBlank()) {
            return null;
        }

        return reference.id().trim();
    }
}
