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
 * Graduate examinations (CW-001-MAP-000816). Only a master or PhD examination has a thesis in
 * TeslaRIS; the rest are logged. The student is not a TeslaRIS person, so the education fields the
 * crosswalk maps to the student are logged as well.
 */
@Component
@RequiredArgsConstructor
public class HydratorExaminationExtractor
    implements RecordExtractor<HydratorCVModel.Curriculum, ThesisContributionMigrationDTO> {

    private final HydratorOutputConverters outputConverters;

    private final MigrationLog migrationLog;


    @Override
    public List<ThesisContributionMigrationDTO> extract(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().services()) ||
            Objects.isNull(record.curriculum().services().service())) {
            return List.of();
        }

        var dtos = new ArrayList<ThesisContributionMigrationDTO>();

        record.curriculum().services().service().forEach(service -> {
            if (Objects.isNull(service) || Objects.isNull(service.graduateExamination())) {
                return;
            }

            if (Objects.isNull(service.id()) || service.id().isBlank()) {
                dropped(record.id(), HydratorMappingRule.cv(816), "examination without id");
                return;
            }

            var key = outputConverters.examinationKey(record, service);
            var examination = service.graduateExamination();
            var skipReason = skipReason(examination);

            if (Objects.nonNull(skipReason)) {
                dropped(key, HydratorMappingRule.cv(816), skipReason);
                return;
            }

            dropValue(key, HydratorMappingRule.cv(818), examination.tid(),
                "thesis has no tid field");
            dropValue(key, HydratorMappingRule.cv(823), examination.examinationSubject(),
                "thesis has no examination subject field");
            dropValue(key, HydratorMappingRule.cv(824), examination.courseCode(),
                "student is not a person, no education to hold the course code");

            dtos.add(new ThesisContributionMigrationDTO(record.id(), key,
                outputConverters.toExaminationThesis(record, service),
                outputConverters.studentName(examination.studentName()), null));
        });

        return dtos;
    }

    private String skipReason(HydratorCVModel.Examination examination) {
        if (!outputConverters.isThesisTitle(examination.theme())) {
            return "thesis not created (no thesis title): '" +
                Objects.toString(examination.theme(), "") + "'";
        }

        if (Objects.isNull(
            HydratorDegreeTypes.serviceThesisType(examination.examinationDegree()))) {
            var type = Objects.isNull(examination.examinationDegree()) ? null :
                examination.examinationDegree().value();
            return "thesis not created (degree type '" + type + "' has no thesis)";
        }

        if (Objects.isNull(outputConverters.studentName(examination.studentName()))) {
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
            MigrationEntityType.THESIS_EXAMINATION.name(), key, rule, reason);
    }
}
