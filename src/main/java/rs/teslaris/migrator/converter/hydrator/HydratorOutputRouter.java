package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.EntityUpdater;
import rs.teslaris.migrator.pipeline.ExistingReader;
import rs.teslaris.migrator.pipeline.FailureHandler;
import rs.teslaris.migrator.pipeline.ItemRouter;
import rs.teslaris.migrator.pipeline.MigrationItem;
import rs.teslaris.migrator.pipeline.UpdatePlan;
import rs.teslaris.migrator.pipeline.failure.DocumentDuplicateFailureHandler;
import rs.teslaris.migrator.util.MigrationEntityType;
import rs.teslaris.migrator.util.ResolvedToExistingException;

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

    private final MonographPublicationService monographPublicationService;

    private final HydratorMonographResolver monographResolver;

    private final HydratorDocumentResolver documentResolver;

    private final MonographService monographService;

    private final HydratorPublisherResolver publisherResolver;

    private final HydratorContributionLinker contributionLinker;

    private final HydratorPersonResolver personResolver;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentDuplicateFailureHandler duplicateFailureHandler;

    private final StudentThesisEntityCreator studentThesisEntityCreator;


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
                null,
                dto,
                (article, run) -> {
                    requireUnusedIdentifiers(article, null);
                    resolveSameWork(MigrationEntityType.JOURNAL_PUBLICATION, record, article,
                        journalPublicationService::readJournalPublicationById,
                        journalPublicationService::editJournalPublication);
                    article.setJournalId(converters.journalId(record, output));
                    return registered(MigrationEntityType.JOURNAL_PUBLICATION, article,
                        journalPublicationService.createJournalPublication(article,
                            run.isPerformIndex()).getId());
                },
                UpdatePlan.<JournalPublicationDTO>of(
                    ExistingReader.of(journalPublicationService::readJournalPublicationById),
                    (targetId, article, run) -> {
                        requireUnusedIdentifiers(article, targetId);
                        journalPublicationService.editJournalPublication(targetId, article);
                    }),
                duplicateFailureHandler.forType(JournalPublicationDTO.class));
        }

        if (Objects.nonNull(output.conferencePaper()) ||
            Objects.nonNull(output.conferenceAbstract())) {
            var dto = Objects.nonNull(output.conferencePaper()) ?
                converters.toProceedingsPublication(record, output) :
                converters.toConferenceAbstract(record, output);
            var type = Objects.nonNull(output.conferencePaper()) ?
                MigrationEntityType.PROCEEDINGS_PUBLICATION :
                MigrationEntityType.CONFERENCE_ABSTRACT;

            // Proceedings are resolved on creation, so skipped or filtered items create no stubs
            return Objects.isNull(dto) ? null : new MigrationItem<ProceedingsPublicationDTO>(
                type,
                converters.outputKey(record, output),
                null,
                dto,
                (publication, run) -> {
                    requireUnusedIdentifiers(publication, null);
                    resolveSameWork(type, record, publication,
                        proceedingsPublicationService::readProceedingsPublicationById,
                        proceedingsPublicationService::editProceedingsPublication);
                    var container =
                        proceedingsResolver.resolveOrCreate(record, output, run.isPerformIndex());
                    publication.setProceedingsId(container.proceedingsId());
                    publication.setEventId(container.eventId());
                    return registered(type, publication,
                        proceedingsPublicationService.createProceedingsPublication(
                            publication, run.isPerformIndex()).getId());
                },
                // The merge keeps the present proceedings and event, so an update creates no
                // second container for the same paper.
                UpdatePlan.<ProceedingsPublicationDTO>of(
                    ExistingReader.of(
                        proceedingsPublicationService::readProceedingsPublicationById),
                    (targetId, publication, run) -> {
                        requireUnusedIdentifiers(publication, targetId);
                        proceedingsPublicationService.editProceedingsPublication(targetId,
                            publication);
                    }),
                duplicateFailureHandler.forType(ProceedingsPublicationDTO.class));
        }

        if (Objects.nonNull(output.conferencePoster())) {
            var dto = converters.toConferencePoster(record, output);

            // The conference is resolved on creation, so skipped posters create no stubs
            return Objects.isNull(dto) ? null : new MigrationItem<PerformanceRelatedOutputDTO>(
                MigrationEntityType.CONFERENCE_POSTER,
                converters.outputKey(record, output),
                null,
                dto,
                (poster, run) -> {
                    dropUsedIdentifiers(record, output, poster, null);
                    resolveSameWork(MigrationEntityType.CONFERENCE_POSTER, record, poster,
                        performanceRelatedOutputService::readPerformanceRelatedOutputById,
                        performanceRelatedOutputService::editPerformanceRelatedOutput);
                    poster.setEventId(
                        proceedingsResolver.resolveConference(record, output,
                            run.isPerformIndex()));
                    return registered(MigrationEntityType.CONFERENCE_POSTER, poster,
                        performanceRelatedOutputService.createPerformanceRelatedOutput(poster,
                            run.isPerformIndex()).getId());
                },
                // The merge keeps the present conference, so an update creates no second stub
                UpdatePlan.<PerformanceRelatedOutputDTO>of(
                    ExistingReader.of(
                        performanceRelatedOutputService::readPerformanceRelatedOutputById),
                    (targetId, poster, run) -> {
                        dropUsedIdentifiers(record, output, poster, targetId);
                        performanceRelatedOutputService.editPerformanceRelatedOutput(targetId,
                            poster);
                    }),
                duplicateFailureHandler.forType(PerformanceRelatedOutputDTO.class));
        }

        if (Objects.nonNull(output.bookChapter())) {
            var dto = converters.toBookChapter(record, output);

            // The book is resolved on creation, so skipped or filtered chapters create no stubs
            return Objects.isNull(dto) ? null : new MigrationItem<MonographPublicationDTO>(
                MigrationEntityType.MONOGRAPH_PUBLICATION,
                converters.outputKey(record, output),
                null,
                dto,
                (chapter, run) -> {
                    requireUnusedIdentifiers(chapter, null);
                    resolveSameWork(MigrationEntityType.MONOGRAPH_PUBLICATION, record, chapter,
                        monographPublicationService::readMonographPublicationById,
                        monographPublicationService::editMonographPublication);
                    chapter.setMonographId(
                        monographResolver.resolveOrCreate(record, output, run.isPerformIndex()));
                    return registered(MigrationEntityType.MONOGRAPH_PUBLICATION, chapter,
                        monographPublicationService.createMonographPublication(chapter,
                            run.isPerformIndex()).getId());
                },
                // The merge keeps the present book, so an update creates no second stub
                UpdatePlan.<MonographPublicationDTO>of(
                    ExistingReader.of(
                        monographPublicationService::readMonographPublicationById),
                    (targetId, chapter, run) -> {
                        requireUnusedIdentifiers(chapter, targetId);
                        monographPublicationService.editMonographPublication(targetId, chapter);
                    }),
                duplicateFailureHandler.forType(MonographPublicationDTO.class));
        }

        if (Objects.nonNull(output.book())) {
            var dto = converters.toBook(record, output);

            // The publisher is resolved on creation, so skipped or filtered books create no stubs
            return Objects.isNull(dto) ? null : new MigrationItem<MonographDTO>(
                MigrationEntityType.MONOGRAPH,
                converters.outputKey(record, output),
                null,
                dto,
                (book, run) -> {
                    requireUnusedIdentifiers(book, null);

                    var existing = monographResolver.existingBook(output, book);
                    if (existing.isPresent()) {
                        throw new ResolvedToExistingException(existing.get(),
                            monographResolver.mergeBook(record, output, existing.get(), book));
                    }

                    resolveSameWork(MigrationEntityType.MONOGRAPH, record, book,
                        monographService::readMonographById, monographService::editMonograph);
                    book.setPublisherId(publisherResolver.resolveOrCreate(
                        output.book().publisher(), converters.languageOf(record),
                        output.book().publicationLocation()));
                    return registered(MigrationEntityType.MONOGRAPH, book,
                        monographService.createMonograph(book, run.isPerformIndex()).getId());
                },
                UpdatePlan.<MonographDTO>of(
                    ExistingReader.of(monographService::readMonographById),
                    (targetId, book, run) -> {
                        requireUnusedIdentifiers(book, targetId);
                        monographService.editMonograph(targetId, book);
                    }),
                duplicateFailureHandler.forType(MonographDTO.class));
        }

        if (Objects.nonNull(output.dissertation())) {
            var dto = converters.toThesis(record, output);

            return Objects.isNull(dto) ? null : new MigrationItem<>(
                MigrationEntityType.THESIS,
                converters.outputKey(record, output),
                null,
                dto,
                studentThesisEntityCreator,
                UpdatePlan.<ThesisDTO>of(ExistingReader.of(thesisService::readThesisById),
                    EntityUpdater.of(thesisService::editThesis)),
                FailureHandler.resolvedToExisting());
        }

        return null;
    }

    // CW-001-MAP-000090/091: a presented conference paper also yields its slides, as an item of
    // their own
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
            null,
            dto,
            (slides, run) -> {
                slides.setEventId(
                    proceedingsResolver.createPresentationEvent(record, output,
                        run.isPerformIndex()));
                return performanceRelatedOutputService.createPerformanceRelatedOutput(slides,
                    run.isPerformIndex()).getId();
            },
            // The merge keeps the present event, so an update creates no second event.
            UpdatePlan.<PerformanceRelatedOutputDTO>of(
                ExistingReader.of(
                    performanceRelatedOutputService::readPerformanceRelatedOutputById),
                EntityUpdater.of(performanceRelatedOutputService::editPerformanceRelatedOutput)),
            FailureHandler.noOp());
    }

    /**
     * The same work from a co-author's curriculum, matched by title, year and authors where no
     * identifier ties the two: the owner is linked on the existing document and no second one is
     * created.
     */
    private <D extends DocumentDTO> void resolveSameWork(MigrationEntityType type,
                                                         HydratorCVModel.Curriculum record,
                                                         D dto,
                                                         Function<Integer, ? extends D> reader,
                                                         BiConsumer<Integer, D> updater) {
        var existing = documentResolver.find(type, dto, personResolver.resolveOwner(record));

        if (existing.isPresent()) {
            throw new ResolvedToExistingException(existing.get(),
                "same work in a co-author's curriculum: " +
                    contributionLinker.link(existing.get(), dto, reader, updater));
        }
    }

    private Integer registered(MigrationEntityType type, DocumentDTO dto, Integer documentId) {
        documentResolver.register(type, documentId, dto);
        return documentId;
    }

    private void dropUsedIdentifiers(HydratorCVModel.Curriculum record,
                                     HydratorCVModel.Output output, DocumentDTO dto,
                                     Integer targetId) {
        documentPublicationService.findDocumentByCommonIdentifier(dto.getDoi(),
                dto.getOpenAlexId(), dto.getScopusId(), dto.getWebOfScienceId())
            .filter(owner -> !Objects.equals(owner.getId(), targetId))
            .ifPresent(owner -> converters.dropUsedIdentifiers(record, output, dto,
                owner.getId()));
    }

    // A duplicate fails before its container is resolved, so it leaves no stubs behind
    private void requireUnusedIdentifiers(DocumentDTO dto, Integer targetId) {
        var owner = documentPublicationService.findDocumentByCommonIdentifier(dto.getDoi(),
            dto.getOpenAlexId(), dto.getScopusId(), dto.getWebOfScienceId());

        if (owner.isPresent() && !Objects.equals(owner.get().getId(), targetId)) {
            throw new IdentifierException("identifierExistsError");
        }
    }
}
