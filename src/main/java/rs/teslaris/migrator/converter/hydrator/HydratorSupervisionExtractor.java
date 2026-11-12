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
 * Research-based degree supervisions (CW-001-MAP-000862). Only a completed bachelor, master or PhD
 * supervision has a thesis in TeslaRIS; the rest are logged. The student is not a TeslaRIS person,
 * so the education fields the crosswalk maps to the student are logged as well.
 */
@Component
@RequiredArgsConstructor
public class HydratorSupervisionExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, SupervisionMigrationDTO> {

    private static final String COMPLETED = "Completed";

    private final HydratorOutputConverters outputConverters;

    private final MigrationLog migrationLog;


    @Override
    public List<SupervisionMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().services()) ||
            Objects.isNull(record.curriculum().services().service())) {
            return List.of();
        }

        var dtos = new ArrayList<SupervisionMigrationDTO>();

        record.curriculum().services().service().forEach(service -> {
            if (Objects.isNull(service) ||
                Objects.isNull(service.researchBasedDegreeSupervision())) {
                return;
            }

            if (Objects.isNull(service.id()) || service.id().isBlank()) {
                dropped(record.id(), HydratorMappingRule.cv(862), "supervision without id");
                return;
            }

            var key = outputConverters.supervisionKey(record, service);
            var supervision = service.researchBasedDegreeSupervision();
            var skipReason = skipReason(supervision);

            if (Objects.nonNull(skipReason)) {
                dropped(key, HydratorMappingRule.cv(862), skipReason);
                return;
            }

            dropValue(key, HydratorMappingRule.cv(865), supervision.tid(),
                "thesis has no tid field");
            dropValue(key, HydratorMappingRule.cv(868), supervision.degreeSubject(),
                "student is not a person, no education to hold the degree title");

            dtos.add(new SupervisionMigrationDTO(record.id(), key,
                outputConverters.toSupervisionThesis(record, service),
                outputConverters.studentName(supervision), null));
        });

        return dtos;
    }

    private String skipReason(HydratorCVModel.Supervision supervision) {
        if (!outputConverters.isThesisTitle(supervision.thesisTitle())) {
            return "thesis not created (no thesis title): '" +
                Objects.toString(supervision.thesisTitle(), "") + "'";
        }

        var status = Objects.isNull(supervision.degreeStatus()) ? null :
            supervision.degreeStatus().status();
        if (!COMPLETED.equalsIgnoreCase(Objects.toString(status, "").trim())) {
            return "thesis not created (supervision not completed): '" + status + "'";
        }

        if (Objects.isNull(HydratorDegreeTypes.supervisionThesisType(supervision.degreeType()))) {
            var type = Objects.isNull(supervision.degreeType()) ? null :
                supervision.degreeType().value();
            return "thesis not created (degree type '" + type + "' has no thesis)";
        }

        if (Objects.isNull(outputConverters.studentName(supervision))) {
            return "thesis not created (no student name)";
        }

        return null;
    }

    private void dropValue(String key, String rule, String value, String reason) {
        if (Objects.nonNull(value) && !value.isBlank()) {
            dropped(key, rule, reason + ": '" + value.trim() + "'");
        }
    }

    private void dropped(String key, String rule, String reason) {
        migrationLog.valueDropped(HydratorSource.NAME,
            MigrationEntityType.THESIS_SUPERVISION.name(), key, rule, reason);
    }
}
