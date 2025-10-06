package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ScriptQuery;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.annotation.Traceable;
import rs.teslaris.core.indexmodel.BookSeriesIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.OrganisationUnitIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationBlacklist;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;
import rs.teslaris.core.indexrepository.BookSeriesIndexRepository;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.indexrepository.OrganisationUnitIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
import rs.teslaris.core.indexrepository.PublisherIndexRepository;
import rs.teslaris.core.indexrepository.deduplication.DocumentDeduplicationBlacklistRepository;
import rs.teslaris.core.indexrepository.deduplication.DocumentDeduplicationSuggestionRepository;
import rs.teslaris.core.service.interfaces.commontypes.NotificationService;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.user.UserService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.util.notificationhandling.NotificationFactory;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Traceable
public class DeduplicationServiceImpl implements DeduplicationService {

    private static final Integer CHUNK_SIZE = 20;

    private static volatile boolean deduplicationLock = false;

    private final AtomicInteger currentSessionCounter = new AtomicInteger(0);

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final PublisherIndexRepository publisherIndexRepository;

    private final EventIndexRepository eventIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final DocumentDeduplicationSuggestionRepository deduplicationSuggestionRepository;

    private final DocumentDeduplicationBlacklistRepository documentDeduplicationBlacklistRepository;

    private final UserService userService;

    private final NotificationService notificationService;

    private final SearchService<DocumentPublicationIndex> documentSearchService;

    private final SearchService<PublisherIndex> publisherSearchService;

    private final SearchService<JournalIndex> journalSearchService;

    private final SearchService<BookSeriesIndex> bookSeriesSearchService;

    private final BookSeriesIndexRepository bookSeriesIndexRepository;

    private final SearchService<OrganisationUnitIndex> organisationUnitSearchService;

    private final OrganisationUnitIndexRepository organisationUnitIndexRepository;

    private final SearchService<EventIndex> eventSearchService;

    private final SearchService<PersonIndex> personSearchService;

    @Value("${deduplication.allowed}")
    private Boolean deduplicationAllowed;


    @Override
    public void deleteSuggestion(String suggestionId) {
        deduplicationSuggestionRepository.delete(
            findDeduplicationSuggestionById(suggestionId));
    }

    @Override
    public void deleteSuggestion(Integer deletedEntityId, EntityType entityType) {
        deduplicationSuggestionRepository.deleteAll(
            deduplicationSuggestionRepository.findByEntityIdAndEntityType(deletedEntityId,
                entityType.name()));
    }

    @Override
    public void flagAsNotDuplicate(String suggestionId) {
        var suggestion = findDeduplicationSuggestionById(suggestionId);

        var blacklistEntry =
            documentDeduplicationBlacklistRepository.findByEntityIdsAndEntityType(
                suggestion.getLeftEntityId(), suggestion.getRightEntityId(),
                suggestion.getEntityType().name());

        if (blacklistEntry.isEmpty()) {
            documentDeduplicationBlacklistRepository.save(
                new DeduplicationBlacklist(suggestion.getLeftEntityId(),
                    suggestion.getRightEntityId(), suggestion.getEntityType()));
        }

        deleteSuggestion(suggestionId);
    }

    @Override
    public Page<DeduplicationSuggestion> getDeduplicationSuggestions(Pageable pageable,
                                                                     EntityType type) {
        return deduplicationSuggestionRepository.findByEntityType(type.name(), pageable);
    }

    @Override
    public boolean canPerformDeduplication() {
        return !DeduplicationServiceImpl.deduplicationLock;
    }

    @Async("taskExecutor")
    public void startDeduplicationAsync(Integer initiatingUserId) {
        try {
            performAllScheduledDeduplicationProcesses();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            notificationService.createNotification(
                NotificationFactory.contructNewDeduplicationScanFinishedNotification(
                    Map.of("duplicateCount", currentSessionCounter.toString()),
                    userService.findOne(initiatingUserId))
            );
            deduplicationLock = false;
        }
    }

    @Scheduled(cron = "${deduplication.schedule}")
    protected synchronized void performAllScheduledDeduplicationProcesses() {
        if (!deduplicationAllowed) {
            return;
        }

        if (deduplicationLock) {
            log.info("Deduplication startup aborted due to process already running.");
            return;
        }

        deduplicationLock = true;
        currentSessionCounter.set(0);

        log.info("Starting all deduplication processes.");

        try {
            var futures = List.of(
                CompletableFuture.runAsync(this::performScheduledDocumentDeduplication),
                CompletableFuture.runAsync(this::performScheduledJournalDeduplication),
                CompletableFuture.runAsync(this::performScheduledEventDeduplication),
                CompletableFuture.runAsync(this::performScheduledPersonDeduplication),
                CompletableFuture.runAsync(this::performScheduledBookSeriesDeduplication),
                CompletableFuture.runAsync(this::performScheduledOrganisationUnitsDeduplication),
                CompletableFuture.runAsync(this::performScheduledPublisherDeduplication)
            );

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            deduplicationLock = false;
            log.info("All deduplication processes completed. Total duplicates found: {}.",
                currentSessionCounter);
        }
    }

    private void performScheduledDocumentDeduplication() {
        performScheduledDeduplication(
            EntityType.PUBLICATION.name(),
            (pageNumber) -> documentPublicationIndexRepository.findByTypeIn(
                List.of(
                    DocumentPublicationType.MONOGRAPH.name(),
                    DocumentPublicationType.MONOGRAPH_PUBLICATION.name(),
                    DocumentPublicationType.PROCEEDINGS.name(),
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(),
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    DocumentPublicationType.PATENT.name(),
                    DocumentPublicationType.SOFTWARE.name(),
                    DocumentPublicationType.DATASET.name(),
                    DocumentPublicationType.THESIS.name()
                ),
                PageRequest.of(pageNumber, CHUNK_SIZE)).getContent(),
            documentSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_sr").query(item.getTitleSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_other").query(item.getTitleOther())));
                        return eq;
                    });
                    return bq;
                });

                if (item.getType().equals("PROCEEDINGS")) {
                    b.must(sb -> sb.term(
                        m -> m.field("event_id").value(item.getEventId())));
                }

                if (item.getType().equals("PROCEEDINGS_PUBLICATION") ||
                    item.getType().equals("JOURNAL_PUBLICATION")) {
                    b.must(sb -> sb.term(
                        m -> m.field("publication_type").value(item.getPublicationType())));
                }

                b.must(sb -> sb.match(
                    m -> m.field("type").query(item.getType())));
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query(item.getDatabaseId())));
                return b;
            }))),
            DocumentPublicationIndex::getDatabaseId,
            DocumentPublicationIndex::getTitleSr,
            DocumentPublicationIndex::getTitleOther,
            DocumentPublicationIndex::getType,
            "document_publication"
        );
    }

    private void performScheduledJournalDeduplication() {
        performScheduledDeduplication(
            EntityType.JOURNAL.name(),
            (pageNumber) -> journalIndexRepository.findAll(PageRequest.of(pageNumber, CHUNK_SIZE))
                .getContent(),
            journalSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_sr").query((item).getTitleSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_other").query((item).getTitleOther())));
                        return eq;
                    });
                    return bq;
                });
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query((item).getDatabaseId())));
                b.must(sb -> sb.script(ScriptQuery.of(sq -> sq.script(s -> s.inline(i -> i.source(
                        "doc['title_sr_sortable'].value.length() == " + item.getTitleSr().length() +
                            " && doc['title_other_sortable'].value.length() == " +
                            item.getTitleOther().length())
                    ))
                )));
                return b;
            }))),
            JournalIndex::getDatabaseId,
            JournalIndex::getTitleSr,
            JournalIndex::getTitleOther,
            null,
            "journal"
        );
    }

    private void performScheduledPublisherDeduplication() {
        performScheduledDeduplication(
            EntityType.PUBLISHER.name(),
            (pageNumber) -> publisherIndexRepository.findAll(PageRequest.of(pageNumber, CHUNK_SIZE))
                .getContent(),
            publisherSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_sr").query((item).getNameSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_other").query((item).getNameOther())));
                        return eq;
                    });
                    return bq;
                });
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query((item).getDatabaseId())));
                b.must(sb -> sb.script(ScriptQuery.of(sq -> sq.script(s -> s.inline(i -> i.source(
                        "doc['name_sr_sortable'].value.length() == " + item.getNameSr().length() +
                            " && doc['name_other_sortable'].value.length() == " +
                            item.getNameOther().length())
                    ))
                )));
                return b;
            }))),
            PublisherIndex::getDatabaseId,
            PublisherIndex::getNameSr,
            PublisherIndex::getNameOther,
            null,
            "publisher"
        );
    }

    private void performScheduledBookSeriesDeduplication() {
        performScheduledDeduplication(
            EntityType.BOOK_SERIES.name(),
            (pageNumber) -> bookSeriesIndexRepository.findAll(
                PageRequest.of(pageNumber, CHUNK_SIZE)).getContent(),
            bookSeriesSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_sr").query((item).getTitleSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("title_other").query((item).getTitleOther())));
                        return eq;
                    });
                    return bq;
                });
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query((item).getDatabaseId())));
                b.must(sb -> sb.script(ScriptQuery.of(sq -> sq.script(s -> s.inline(i -> i.source(
                        "doc['title_sr_sortable'].value.length() == " + item.getTitleSr().length() +
                            " && doc['title_other_sortable'].value.length() == " +
                            item.getTitleOther().length())
                    ))
                )));
                return b;
            }))),
            BookSeriesIndex::getDatabaseId,
            BookSeriesIndex::getTitleSr,
            BookSeriesIndex::getTitleOther,
            null,
            "book_series"
        );
    }

    private void performScheduledOrganisationUnitsDeduplication() {
        performScheduledDeduplication(
            EntityType.ORGANISATION_UNIT.name(),
            (pageNumber) -> organisationUnitIndexRepository.findAll(
                PageRequest.of(pageNumber, CHUNK_SIZE)).getContent(),
            organisationUnitSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_sr").query((item).getNameSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_other").query((item).getNameOther())));
                        return eq;
                    });
                    return bq;
                });
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query((item).getDatabaseId())));

                if (Objects.nonNull(item.getSuperOUNameSr()) &&
                    Objects.nonNull(item.getSuperOUNameOther())) {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            eq.should(sb -> sb.match(
                                m -> m.field("super_ou_name_sr").query((item).getSuperOUNameSr())));
                            eq.should(sb -> sb.match(
                                m -> m.field("super_ou_name_other")
                                    .query((item).getSuperOUNameOther())));
                            return eq;
                        });
                        return bq;
                    });
                }

                return b;
            }))),
            OrganisationUnitIndex::getDatabaseId,
            OrganisationUnitIndex::getNameSr,
            OrganisationUnitIndex::getNameOther,
            null,
            "organisation_unit"
        );
    }

    private void performScheduledEventDeduplication() {
        performScheduledDeduplication(
            EntityType.EVENT.name(),
            (pageNumber) -> eventIndexRepository.findAll(PageRequest.of(pageNumber, CHUNK_SIZE))
                .getContent(),
            eventSearchService,
            item -> BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                b.must(bq -> {
                    bq.bool(eq -> {
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_sr").query((item).getNameSr())));
                        eq.should(sb -> sb.matchPhrase(
                            m -> m.field("name_other").query((item).getNameOther())));
                        return eq;
                    });
                    return bq;
                });
                b.mustNot(sb -> sb.match(
                    m -> m.field("databaseId").query((item).getDatabaseId())));
                return b;
            }))),
            EventIndex::getDatabaseId,
            EventIndex::getNameSr,
            EventIndex::getNameOther,
            null,
            "events"
        );
    }

    private void performScheduledPersonDeduplication() {
        performScheduledDeduplication(
            EntityType.PERSON.name(),
            (pageNumber) -> personIndexRepository.findAll(PageRequest.of(pageNumber, CHUNK_SIZE))
                .getContent(),
            personSearchService,
            item -> {
                var tokens = Arrays.stream(item.getName().trim().split("; "))
                    .flatMap(name -> Arrays.stream(name.split(" ")))
                    .map(namePart -> namePart.replace("(", "").replace(")", ""))
                    .toList();

                return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            tokens.forEach(
                                token -> {
                                    if (token.trim().isEmpty()) {
                                        return;
                                    }

                                    eq.should(
                                        sb -> sb.matchPhrase(m -> m.field("name").query(token)));
                                }
                            );

                            if (Objects.nonNull(item.getBirthdate()) &&
                                !item.getBirthdate().isBlank()) {
                                eq.must(sb -> sb.match(
                                    m -> m.field("birthdate").query(item.getBirthdate())));
                            }

                            return eq.minimumShouldMatch(String.valueOf(Math.ceil(
                                0.7 * tokens.stream().filter(String::isBlank).toList().size())));
                        });
                        return bq;
                    });
                    b.mustNot(sb -> sb.match(
                        m -> m.field("databaseId").query(item.getDatabaseId())));
                    return b;
                })));
            },
            PersonIndex::getDatabaseId,
            PersonIndex::getName,
            PersonIndex::getName,
            null,
            "person"
        );
    }

    private <T> void performScheduledDeduplication(
        String indexType,
        Function<Integer, List<T>> fetchChunk,
        SearchService<T> searchService,
        Function<T, BoolQuery> constructQuery,
        Function<T, Integer> getDatabaseId,
        Function<T, String> getTitleSr,
        Function<T, String> getTitleOther,
        Function<T, String> getType,
        String collection
    ) {
        log.info("Deduplication of {} started.", indexType);
        deduplicationSuggestionRepository.deleteByEntityType(indexType);

        int pageNumber = 0;
        boolean hasNextPage = true;
        var duplicatesFound = new ConcurrentSkipListSet<Integer>();

        while (hasNextPage) {
            List<T> chunk = fetchChunk.apply(pageNumber);

            chunk.forEach(item -> {
                if (duplicatesFound.contains(getDatabaseId.apply(item))) {
                    return;
                }

                var deduplicationQuery = constructQuery.apply(item);
                var similarItems = searchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    (Class<T>) item.getClass(),
                    collection
                ).getContent();

                if (!similarItems.isEmpty()) {
                    handleDuplicate(
                        item, similarItems, duplicatesFound,
                        EntityType.valueOf(indexType.toUpperCase()),
                        getDatabaseId, getTitleSr, getTitleOther, getType
                    );
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == CHUNK_SIZE;
        }
    }

    private <T> void handleDuplicate(
        T entity,
        List<T> similarEntities,
        ConcurrentSkipListSet<Integer> foundDuplicates,
        EntityType indexType,
        Function<T, Integer> getIdFunction,
        Function<T, String> getTitleSrFunction,
        Function<T, String> getTitleOtherFunction,
        Function<T, String> getTypeFunction
    ) {
        for (T similarEntity : similarEntities) {

            var blacklistEntry = documentDeduplicationBlacklistRepository
                .findByEntityIdsAndEntityType(
                    getIdFunction.apply(entity),
                    getIdFunction.apply(similarEntity),
                    indexType.name()
                );

            if (blacklistEntry.isPresent()) {
                continue;
            }

            if (!deduplicationSuggestionRepository.findByTwoEntitiesAndType(
                    getIdFunction.apply(entity), getIdFunction.apply(similarEntity), indexType.name())
                .isEmpty()) {
                continue;
            }

            foundDuplicates.add(getIdFunction.apply(similarEntity));
            log.info("Found potential duplicate: {} ({}) == {} ({})",
                getTitleSrFunction.apply(entity),
                getTitleOtherFunction.apply(entity),
                getTitleSrFunction.apply(similarEntity),
                getTitleOtherFunction.apply(similarEntity));

            currentSessionCounter.incrementAndGet();
            deduplicationSuggestionRepository.save(
                new DeduplicationSuggestion(
                    getIdFunction.apply(entity),
                    getIdFunction.apply(similarEntity),
                    getTitleSrFunction.apply(entity),
                    getTitleOtherFunction.apply(entity),
                    getTitleSrFunction.apply(similarEntity),
                    getTitleOtherFunction.apply(similarEntity),
                    indexType == EntityType.PUBLICATION ?
                        DocumentPublicationType.valueOf(getTypeFunction.apply(entity)) :
                        null,
                    indexType
                )
            );
        }
    }

    private DeduplicationSuggestion findDeduplicationSuggestionById(
        String suggestionId) {
        return deduplicationSuggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new NotFoundException("Suggestion with given ID does not exist."));
    }
}
