package rs.teslaris.migrator.converter.hydrator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.commontypes.MultilingualContentDTO;
import rs.teslaris.core.dto.document.DocumentDTO;
import rs.teslaris.core.dto.document.JournalPublicationDTO;
import rs.teslaris.core.dto.document.JournalPublicationResponseDTO;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.dto.document.PerformanceRelatedOutputDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.service.interfaces.document.ConferenceService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalPublicationService;
import rs.teslaris.core.service.interfaces.document.JournalService;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.core.service.interfaces.document.PerformanceRelatedOutputService;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.core.util.exceptionhandling.exception.IdentifierException;
import rs.teslaris.migrator.converter.hydrator.HydratorPidDuplicateMerger.Filler;
import rs.teslaris.migrator.converter.hydrator.HydratorPidDuplicateMerger.TypeRule;
import rs.teslaris.migrator.model.hydrator.HydratorCVModel;
import rs.teslaris.migrator.pipeline.EntityCreator;
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

    private final HydratorSourceWorkIdWriter sourceWorkIdWriter;

    private final HydratorPidDuplicateMerger pidMerger;

    private final JournalService journalService;

    private final ProceedingsService proceedingsService;

    private final ConferenceService conferenceService;

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
                this.<JournalPublicationDTO>keepingSourceWorkId(
                    MigrationEntityType.JOURNAL_PUBLICATION, record, output,
                    (article, run) -> {
                        mergeByPid(MigrationEntityType.JOURNAL_PUBLICATION, record, output,
                            article, journalPublicationService::readJournalPublicationById,
                            journalPublicationService::editJournalPublication, journalRule(output));
                        requireUnusedIdentifiers(article, null);
                        resolveSameWork(MigrationEntityType.JOURNAL_PUBLICATION, record, article,
                            journalPublicationService::readJournalPublicationById,
                            journalPublicationService::editJournalPublication);
                        article.setJournalId(converters.journalId(record, output));
                        return registered(MigrationEntityType.JOURNAL_PUBLICATION, article,
                            journalPublicationService.createJournalPublication(article,
                                run.isPerformIndex()).getId());
                    }),
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
                this.<ProceedingsPublicationDTO>keepingSourceWorkId(
                    type, record, output,
                    (publication, run) -> {
                        mergeByPid(type, record, output, publication,
                            proceedingsPublicationService::readProceedingsPublicationById,
                            proceedingsPublicationService::editProceedingsPublication,
                            proceedingsRule(output));
                        requireUnusedIdentifiers(publication, null);
                        resolveSameWork(type, record, publication,
                            proceedingsPublicationService::readProceedingsPublicationById,
                            proceedingsPublicationService::editProceedingsPublication);
                        var container = proceedingsResolver.resolveOrCreate(record, output,
                            run.isPerformIndex());
                        publication.setProceedingsId(container.proceedingsId());
                        publication.setEventId(container.eventId());
                        return registered(type, publication,
                            proceedingsPublicationService.createProceedingsPublication(
                                publication, run.isPerformIndex()).getId());
                    }),
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
                this.<PerformanceRelatedOutputDTO>keepingSourceWorkId(
                    MigrationEntityType.CONFERENCE_POSTER, record, output,
                    (poster, run) -> {
                        mergeByPid(MigrationEntityType.CONFERENCE_POSTER, record, output, poster,
                            performanceRelatedOutputService::readPerformanceRelatedOutputById,
                            performanceRelatedOutputService::editPerformanceRelatedOutput,
                            posterRule(output));
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
                    }),
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
                this.<MonographPublicationDTO>keepingSourceWorkId(
                    MigrationEntityType.MONOGRAPH_PUBLICATION, record, output,
                    (chapter, run) -> {
                        mergeByPid(MigrationEntityType.MONOGRAPH_PUBLICATION, record, output,
                            chapter, monographPublicationService::readMonographPublicationById,
                            monographPublicationService::editMonographPublication,
                            chapterRule(output));
                        requireUnusedIdentifiers(chapter, null);
                        resolveSameWork(MigrationEntityType.MONOGRAPH_PUBLICATION, record, chapter,
                            monographPublicationService::readMonographPublicationById,
                            monographPublicationService::editMonographPublication);
                        chapter.setMonographId(monographResolver.resolveOrCreate(record, output,
                            run.isPerformIndex()));
                        return registered(MigrationEntityType.MONOGRAPH_PUBLICATION, chapter,
                            monographPublicationService.createMonographPublication(chapter,
                                run.isPerformIndex()).getId());
                    }),
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
                this.<MonographDTO>keepingSourceWorkId(
                    MigrationEntityType.MONOGRAPH, record, output,
                    (book, run) -> {
                        mergeByPid(MigrationEntityType.MONOGRAPH, record, output, book,
                            monographService::readMonographById, monographService::editMonograph,
                            bookRule(record, output));
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
                    }),
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
                this.<ThesisDTO>keepingSourceWorkId(MigrationEntityType.THESIS, record, output,
                    (thesis, run) -> {
                        mergeByPid(MigrationEntityType.THESIS, record, output, thesis,
                            thesisService::readThesisById, thesisService::editThesis,
                            thesisRule());
                        return studentThesisEntityCreator.create(thesis, run);
                    }),
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

    // An obvious duplicate sharing a PID is merged into the existing document (Dragan, 2026-11-19)
    private <D extends DocumentDTO> void mergeByPid(MigrationEntityType type,
                                                    HydratorCVModel.Curriculum record,
                                                    HydratorCVModel.Output output, D dto,
                                                    Function<Integer, ? extends D> reader,
                                                    BiConsumer<Integer, D> updater,
                                                    TypeRule<D> rule) {
        pidMerger.resolve(type, converters.outputKey(record, output), dto, reader, updater, rule);
    }

    private TypeRule<JournalPublicationDTO> journalRule(HydratorCVModel.Output output) {
        var article = output.journalArticle();

        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(JournalPublicationDTO existing) {
                var names = existing instanceof JournalPublicationResponseDTO response ?
                    response.getJournalName() : List.<MultilingualContentDTO>of();
                var codes = new ArrayList<String>();

                if (Objects.nonNull(existing.getJournalId())) {
                    var journal = journalService.readJournal(existing.getJournalId());
                    codes.add(journal.getPrintISSN());
                    codes.add(journal.getEissn());
                }

                return pidMerger.containerDifference("journal", names, codes, article.journal(),
                    Collections.singletonList(
                        converters.partOfIdentifier(article.identifiers(), "issn")));
            }

            @Override
            public void fill(JournalPublicationDTO existing, JournalPublicationDTO incoming,
                             Filler filler) {
                filler.value("volume", existing.getVolume(), incoming.getVolume(),
                    existing::setVolume);
                filler.value("issue", existing.getIssue(), incoming.getIssue(),
                    existing::setIssue);
                filler.value("startPage", existing.getStartPage(), incoming.getStartPage(),
                    existing::setStartPage);
                filler.value("endPage", existing.getEndPage(), incoming.getEndPage(),
                    existing::setEndPage);
                filler.value("numberOfPages", existing.getNumberOfPages(),
                    incoming.getNumberOfPages(), existing::setNumberOfPages);
                filler.value("articleNumber", existing.getArticleNumber(),
                    incoming.getArticleNumber(), existing::setArticleNumber);
            }
        };
    }

    private TypeRule<ProceedingsPublicationDTO> proceedingsRule(HydratorCVModel.Output output) {
        var conferenceName = Objects.nonNull(output.conferencePaper()) ?
            output.conferencePaper().conferenceName() :
            output.conferenceAbstract().conferenceName();
        var identifiers = Objects.nonNull(output.conferencePaper()) ?
            output.conferencePaper().identifiers() : output.conferenceAbstract().identifiers();

        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(ProceedingsPublicationDTO existing) {
                if (Objects.isNull(existing.getProceedingsId())) {
                    return Optional.empty();
                }

                var proceedings = proceedingsService.readProceedingsById(
                    existing.getProceedingsId());

                return pidMerger.containerDifference("conference", proceedings.getEventName(),
                    Arrays.asList(proceedings.getPrintISBN(), proceedings.getEISBN()),
                    conferenceName,
                    Collections.singletonList(converters.proceedingsIsbn(identifiers)));
            }

            @Override
            public void fill(ProceedingsPublicationDTO existing,
                             ProceedingsPublicationDTO incoming, Filler filler) {
                filler.value("startPage", existing.getStartPage(), incoming.getStartPage(),
                    existing::setStartPage);
                filler.value("endPage", existing.getEndPage(), incoming.getEndPage(),
                    existing::setEndPage);
                filler.value("numberOfPages", existing.getNumberOfPages(),
                    incoming.getNumberOfPages(), existing::setNumberOfPages);
                filler.value("articleNumber", existing.getArticleNumber(),
                    incoming.getArticleNumber(), existing::setArticleNumber);
            }
        };
    }

    private TypeRule<PerformanceRelatedOutputDTO> posterRule(HydratorCVModel.Output output) {
        var conferenceName = output.conferencePoster().conferenceName();

        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(PerformanceRelatedOutputDTO existing) {
                if (Objects.isNull(existing.getEventId())) {
                    return Optional.empty();
                }

                return pidMerger.containerDifference("conference",
                    conferenceService.readConference(existing.getEventId()).getName(),
                    List.of(), conferenceName, List.of());
            }

            @Override
            public void fill(PerformanceRelatedOutputDTO existing,
                             PerformanceRelatedOutputDTO incoming, Filler filler) {
                var languages = new HashSet<>(Objects.requireNonNullElse(
                    existing.getLanguageTagIds(), Set.<Integer>of()));

                if (languages.addAll(Objects.requireNonNullElse(incoming.getLanguageTagIds(),
                    Set.<Integer>of()))) {
                    existing.setLanguageTagIds(languages);
                    filler.changed("languages");
                }
            }
        };
    }

    private TypeRule<MonographPublicationDTO> chapterRule(HydratorCVModel.Output output) {
        var chapter = output.bookChapter();

        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(MonographPublicationDTO existing) {
                var codes = new ArrayList<String>();

                if (Objects.nonNull(existing.getMonographId())) {
                    var book = monographService.readMonographById(existing.getMonographId());
                    codes.add(book.getPrintISBN());
                    codes.add(book.getEisbn());
                }

                return pidMerger.containerDifference("book", existing.getMonographName(), codes,
                    chapter.bookTitle(), converters.bookIsbns(chapter.identifiers()));
            }

            @Override
            public void fill(MonographPublicationDTO existing, MonographPublicationDTO incoming,
                             Filler filler) {
                filler.value("startPage", existing.getStartPage(), incoming.getStartPage(),
                    existing::setStartPage);
                filler.value("endPage", existing.getEndPage(), incoming.getEndPage(),
                    existing::setEndPage);
                filler.value("numberOfPages", existing.getNumberOfPages(),
                    incoming.getNumberOfPages(), existing::setNumberOfPages);
                filler.value("articleNumber", existing.getArticleNumber(),
                    incoming.getArticleNumber(), existing::setArticleNumber);
            }
        };
    }

    // A book has no container; its ISBNs are its own identifiers
    private TypeRule<MonographDTO> bookRule(HydratorCVModel.Curriculum record,
                                            HydratorCVModel.Output output) {
        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(MonographDTO existing) {
                // Different only when both carry ISBNs and none is shared
                return pidMerger.containerDifference("isbn", List.of(),
                    Arrays.asList(existing.getPrintISBN(), existing.getEisbn()), null,
                    converters.bookIsbns(output.book().identifiers()));
            }

            @Override
            public void fill(MonographDTO existing, MonographDTO incoming, Filler filler) {
                filler.value("printISBN", existing.getPrintISBN(), incoming.getPrintISBN(),
                    existing::setPrintISBN);
                filler.value("eISBN", existing.getEisbn(), incoming.getEisbn(),
                    existing::setEisbn);
                filler.value("numberOfPages", existing.getNumberOfPages(),
                    incoming.getNumberOfPages(), existing::setNumberOfPages);
                filler.value("volume", existing.getVolume(), incoming.getVolume(),
                    existing::setVolume);
                filler.value("number", existing.getNumber(), incoming.getNumber(),
                    existing::setNumber);
                filler.value("researchArea", existing.getResearchAreaId(),
                    incoming.getResearchAreaId(), existing::setResearchAreaId);
                filler.union("languages", existing.getLanguageIds(), incoming.getLanguageIds(),
                    existing::setLanguageIds);

                if (Objects.isNull(existing.getPublisherId())) {
                    filler.value("publisher", null, publisherResolver.resolveOrCreate(
                            output.book().publisher(), converters.languageOf(record),
                            output.book().publicationLocation()),
                        existing::setPublisherId);
                }
            }
        };
    }

    private TypeRule<ThesisDTO> thesisRule() {
        return new TypeRule<>() {
            @Override
            public Optional<String> containerDifference(ThesisDTO existing) {
                return Optional.empty();
            }

            @Override
            public void fill(ThesisDTO existing, ThesisDTO incoming, Filler filler) {
                filler.value("organisationUnit", existing.getOrganisationUnitId(),
                    incoming.getOrganisationUnitId(), existing::setOrganisationUnitId);
                filler.languages("institution", existing.getExternalOrganisationUnitName(),
                    incoming.getExternalOrganisationUnitName(),
                    existing::setExternalOrganisationUnitName);
                filler.value("defenceDate", existing.getThesisDefenceDate(),
                    incoming.getThesisDefenceDate(), existing::setThesisDefenceDate);
                filler.value("language", existing.getLanguageId(), incoming.getLanguageId(),
                    existing::setLanguageId);
                filler.value("writingLanguage", existing.getWritingLanguageTagId(),
                    incoming.getWritingLanguageTagId(), existing::setWritingLanguageTagId);
                filler.value("numberOfPages", existing.getNumberOfPages(),
                    incoming.getNumberOfPages(), existing::setNumberOfPages);
                filler.languages("scientificArea", existing.getScientificArea(),
                    incoming.getScientificArea(), existing::setScientificArea);
            }
        };
    }

    // The output's source work id is kept on the document it became, created or matched
    private <D> EntityCreator<D> keepingSourceWorkId(MigrationEntityType type,
                                                     HydratorCVModel.Curriculum record,
                                                     HydratorCVModel.Output output,
                                                     EntityCreator<D> creator) {
        return (dto, run) -> {
            Integer documentId;

            try {
                documentId = creator.create(dto, run);
            } catch (ResolvedToExistingException e) {
                sourceWorkIdWriter.attach(type, record, output, e.getEntityId());
                throw e;
            }

            sourceWorkIdWriter.attach(type, record, output, documentId);
            return documentId;
        };
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
