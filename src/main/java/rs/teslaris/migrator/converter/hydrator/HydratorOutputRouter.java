package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.document.Thesis;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
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

    private final ProceedingsPublicationService proceedingsPublicationService;

    private final PerformanceRelatedOutputService performanceRelatedOutputService;

    private final HydratorProceedingsResolver proceedingsResolver;

    private final DocumentPublicationService documentPublicationService;

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

            var presentation = presentationItem(record, output);

            if (Objects.nonNull(presentation)) {
                items.add(presentation);
            }
        });

        return items;
    }

    private MigrationItem<?> routeOutput(HydratorCVModel.Curriculum record,
                                         HydratorCVModel.Output output) {
        if (Objects.nonNull(output.journalArticle())) {
            var dto = converters.toJournalPublication(record, output);

            return Objects.isNull(dto) ? null : new MigrationItem<JournalPublicationDTO>(
                MigrationEntityType.JOURNAL_PUBLICATION,
                converters.outputKey(record, output),
                dto,
                (article, performIndex) -> {
                    requireUnusedIdentifiers(article);
                    article.setJournalId(converters.journalId(record, output));
                    return journalPublicationService.createJournalPublication(article,
                        performIndex).getId();
                },
                duplicateFailureHandler.forType(JournalPublicationDTO.class));
        }

        if (Objects.nonNull(output.conferencePaper())) {
            var dto = converters.toProceedingsPublication(record, output);

            // Proceedings are resolved on creation, so skipped or filtered items create no stubs
            return Objects.isNull(dto) ? null : new MigrationItem<ProceedingsPublicationDTO>(
                MigrationEntityType.PROCEEDINGS_PUBLICATION,
                converters.outputKey(record, output),
                dto,
                (publication, performIndex) -> {
                    requireUnusedIdentifiers(publication);
                    var container =
                        proceedingsResolver.resolveOrCreate(record, output, performIndex);
                    publication.setProceedingsId(container.proceedingsId());
                    publication.setEventId(container.eventId());
                    return proceedingsPublicationService.createProceedingsPublication(
                        publication, performIndex).getId();
                },
                duplicateFailureHandler.forType(ProceedingsPublicationDTO.class));
        }

        if (Objects.nonNull(output.dissertation())) {
            var dto = converters.toThesis(record, output);

            return Objects.isNull(dto) ? null : new MigrationItem<>(
                MigrationEntityType.THESIS,
                converters.outputKey(record, output),
                dto,
                EntityCreator.of(thesisService::createThesis, Thesis::getId),
                FailureHandler.noOp());
        }

        return null;
    }

    // MAP-000090/091: a presented conference paper also yields its slides, as an item of their own
    private MigrationItem<PerformanceRelatedOutputDTO> presentationItem(
        HydratorCVModel.Curriculum record, HydratorCVModel.Output output) {
        if (Objects.isNull(output.conferencePaper())) {
            return null;
        }

        var dto = converters.toPresentation(record, output);

        // The event is created on creation, so skipped or filtered items create no stubs
        return Objects.isNull(dto) ? null : new MigrationItem<PerformanceRelatedOutputDTO>(
            MigrationEntityType.PERFORMANCE_RELATED_OUTPUT,
            converters.outputKey(record, output),
            dto,
            (slides, performIndex) -> {
                slides.setEventId(
                    proceedingsResolver.createPresentationEvent(record, output, performIndex));
                return performanceRelatedOutputService.createPerformanceRelatedOutput(slides,
                    performIndex).getId();
            },
            FailureHandler.noOp());
    }

    // A duplicate fails before its container is resolved, so it leaves no stubs behind
    private void requireUnusedIdentifiers(DocumentDTO dto) {
        if (documentPublicationService.findDocumentByCommonIdentifier(dto.getDoi(),
            dto.getOpenAlexId(), dto.getScopusId(), dto.getWebOfScienceId()).isPresent()) {
            throw new IdentifierException("identifierExistsError");
        }
    }
}
