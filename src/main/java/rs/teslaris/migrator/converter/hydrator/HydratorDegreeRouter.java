package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.EntityUpdater;
import rs.teslaris.migrator.pipeline.ExistingReader;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationItem;
import rs.teslaris.migrator.pipeline.UpdatePlan;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.MigrationLog;

/**
 * Each degree yields its thesis, when it has one of its own, followed by the education pointing
 * at it.
 */
@Component
@RequiredArgsConstructor
public class HydratorDegreeRouter implements ItemRouter<HydratorCVModel.Curriculum> {

    private final HydratorOutputConverters outputConverters;

    private final HydratorEducationExtractor educationExtractor;

    private final EducationEntityCreator educationEntityCreator;

    private final ThesisService thesisService;

    private final MigrationLog migrationLog;


    @Override
    public List<MigrationItem<?>> route(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().degrees()) ||
            Objects.isNull(record.curriculum().degrees().degree())) {
            return List.of();
        }

        var items = new ArrayList<MigrationItem<?>>();

        record.curriculum().degrees().degree().forEach(degree -> {
            if (Objects.isNull(degree) || Objects.isNull(degree.id()) || degree.id().isBlank()) {
                migrationLog.valueDropped(HydratorSource.NAME,
                    MigrationEntityType.PERSON_EDUCATION.name(), record.id(),
                    HydratorMappingRule.cv(38), "degree without id");
                return;
            }

            var key = outputConverters.degreeKey(record, degree);
            var thesis = outputConverters.toDegreeThesis(record, degree);

            if (Objects.nonNull(thesis)) {
                items.add(new MigrationItem<>(
                    MigrationEntityType.THESIS,
                    key,
                    null,
                    thesis,
                    EntityCreator.of(thesisService::createThesis, Thesis::getId),
                    UpdatePlan.<ThesisDTO>of(ExistingReader.of(thesisService::readThesisById),
                        EntityUpdater.of(thesisService::editThesis)),
                    FailureHandler.noOp()));
            }

            items.add(new MigrationItem<>(
                MigrationEntityType.PERSON_EDUCATION,
                key,
                null,
                educationExtractor.toEducation(record, degree, Objects.nonNull(thesis)),
                educationEntityCreator,
                UpdatePlan.of(educationEntityCreator),
                FailureHandler.noOp()));
        });

        return items;
    }
}
