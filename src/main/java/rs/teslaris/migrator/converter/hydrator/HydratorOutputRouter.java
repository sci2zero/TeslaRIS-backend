package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.model.document.JournalPublication;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.EntityCreator;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationItem;
import rs.teslaris.migrator.pipeline.failure.DocumentDuplicateFailureHandler;
import rs.teslaris.migrator.util.MigrationEntityType;

/**
 * The single place encoding "which populated output field means which converter and which core
 * service". Adding support for books, book chapters, conference papers and the rest is a matter of
 * extending the chain below.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HydratorOutputRouter implements ItemRouter<HydratorCVModel.Curriculum> {

    private final HydratorOutputConverters converters;

    private final JournalPublicationService journalPublicationService;

    private final ThesisService thesisService;

    private final DocumentDuplicateFailureHandler duplicateFailureHandler;


    @Override
    public List<MigrationItem<?>> route(HydratorCVModel.Curriculum record) {
        if (Objects.isNull(record.curriculum()) ||
            Objects.isNull(record.curriculum().outputs()) ||
            Objects.isNull(record.curriculum().outputs().output())) {
            return List.of();
        }

        var items = new ArrayList<MigrationItem<?>>();

        record.curriculum().outputs().output().forEach(output -> {
            var item = routeOutput(record, output);

            if (Objects.nonNull(item)) {
                items.add(item);
            } else {
                log.debug("Unsupported or empty output '{}' in curriculum '{}', skipping.",
                    output.id(), record.id());
            }
        });

        return items;
    }

    private MigrationItem<?> routeOutput(HydratorCVModel.Curriculum record,
                                         HydratorCVModel.Output output) {
        if (Objects.nonNull(output.journalArticle())) {
            var dto = converters.toJournalPublication(record, output);

            return Objects.isNull(dto) ? null : new MigrationItem<>(
                MigrationEntityType.JOURNAL_PUBLICATION,
                outputKey(record, output),
                dto,
                EntityCreator.of(
                    journalPublicationService::createJournalPublication, JournalPublication::getId),
                duplicateFailureHandler.forType(JournalPublicationDTO.class));
        }

        if (Objects.nonNull(output.dissertation())) {
            var dto = converters.toThesis(record, output);

            return Objects.isNull(dto) ? null : new MigrationItem<>(
                MigrationEntityType.THESIS,
                outputKey(record, output),
                dto,
                EntityCreator.of(thesisService::createThesis, Thesis::getId),
                FailureHandler.noOp());
        }

        return null;
    }

    /**
     * Output ids are unique within a curriculum only, so the key is composite. The same paper listed
     * in several co-authors' curricula therefore produces different keys - cross-curriculum
     * duplicates are caught by the duplicate failure handler, not by the record log.
     */
    private String outputKey(HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        return record.id() + "#output#" + Objects.toString(output.id(), "unknown");
    }
}
