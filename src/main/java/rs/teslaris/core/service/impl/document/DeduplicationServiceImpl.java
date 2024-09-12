package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.IndexType;
import rs.teslaris.core.indexmodel.JournalIndex;
import rs.teslaris.core.indexmodel.PersonIndex;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationBlacklist;
import rs.teslaris.core.indexmodel.deduplication.DeduplicationSuggestion;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.indexrepository.EventIndexRepository;
import rs.teslaris.core.indexrepository.JournalIndexRepository;
import rs.teslaris.core.indexrepository.PersonIndexRepository;
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
public class DeduplicationServiceImpl implements DeduplicationService {

    private static volatile boolean deduplicationLock = false;
    private static Integer currentSessionCounter = 0;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final JournalIndexRepository journalIndexRepository;

    private final EventIndexRepository eventIndexRepository;

    private final PersonIndexRepository personIndexRepository;

    private final DocumentDeduplicationSuggestionRepository deduplicationSuggestionRepository;

    private final DocumentDeduplicationBlacklistRepository documentDeduplicationBlacklistRepository;

    private final UserService userService;

    private final NotificationService notificationService;

    private final SearchService<DocumentPublicationIndex> documentSearchService;

    private final SearchService<JournalIndex> journalSearchService;

    private final SearchService<EventIndex> eventSearchService;

    private final SearchService<PersonIndex> personSearchService;


    @Override
    public boolean startDeduplicationProcessBeforeSchedule(Integer initiatingUserId) {
        log.info("Trying to start deduplication ahead of time.");
        if (deduplicationLock) {
            return false;
        }

        startDeduplicationAsync(initiatingUserId);
        return true;
    }

    @Override
    public void deleteSuggestion(String suggestionId) {
        deduplicationSuggestionRepository.delete(
            findDeduplicationSuggestionById(suggestionId));
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
                                                                     IndexType type) {
        return deduplicationSuggestionRepository.findByEntityType(type.name(), pageable);
    }

    @Async
    private void startDeduplicationAsync(Integer initiatingUserId) {
        var interrupted = false;
        try {
            performAllScheduledDeduplicationProcesses();
        } catch (Exception e) {
            interrupted = true;
        } finally {
            if (!interrupted) {
                notificationService.createNotification(
                    NotificationFactory.contructNewDeduplicationScanFinishedNotification(
                        Map.of("duplicateCount", currentSessionCounter.toString()),
                        userService.findOne(initiatingUserId))
                );
            }
            deduplicationLock = false;
        }
    }

    @Scheduled(cron = "${deduplication.schedule}")
    protected synchronized void performAllScheduledDeduplicationProcesses() {
        if (deduplicationLock) {
            log.info("Deduplication startup aborted due to process already running.");
            return;
        }

        deduplicationLock = true;
        currentSessionCounter = 0;

        log.info("Starting all deduplication processes.");

        performScheduledDocumentDeduplication();
        performScheduledJournalDeduplication();
        performScheduledEventDeduplication();
        performScheduledPersonDeduplication();

        deduplicationLock = false;

        log.info("All deduplication processes. Total duplicates found: {}.",
            currentSessionCounter);
    }

    protected synchronized void performScheduledDocumentDeduplication() {
        log.info("Deduplication of publications started.");
        deduplicationSuggestionRepository.deleteByEntityType(IndexType.PUBLICATION.name());

        int pageNumber = 0;
        int chunkSize = 20;
        boolean hasNextPage = true;
        var duplicatesFound = new ArrayList<Integer>();

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk = documentPublicationIndexRepository.findByTypeIn(
                List.of(
                    DocumentPublicationType.MONOGRAPH.name(),
                    DocumentPublicationType.MONOGRAPH_PUBLICATION.name(),
                    DocumentPublicationType.PROCEEDINGS.name(),
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(),
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    DocumentPublicationType.PATENT.name(), DocumentPublicationType.SOFTWARE.name(),
                    DocumentPublicationType.DATASET.name(),
                    DocumentPublicationType.THESIS.name()
                ),
                PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(publication -> {
                if (duplicatesFound.contains(publication.getDatabaseId())) {
                    return;
                }

                var deduplicationQuery = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_sr").query(publication.getTitleSr())));
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_other").query(publication.getTitleOther())));
                            return eq;
                        });
                        return bq;
                    });
                    b.must(sb -> sb.match(
                        m -> m.field("type").query(publication.getType())));
                    b.mustNot(sb -> sb.match(
                        m -> m.field("databaseId").query(publication.getDatabaseId())));
                    return b;
                })));

                var similarPublications = documentSearchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    DocumentPublicationIndex.class,
                    "document_publication"
                ).getContent();

                if (!similarPublications.isEmpty()) {
                    handleDuplicate(publication, similarPublications, duplicatesFound,
                        IndexType.PUBLICATION, DocumentPublicationIndex::getDatabaseId,
                        DocumentPublicationIndex::getTitleSr,
                        DocumentPublicationIndex::getTitleOther, DocumentPublicationIndex::getType
                    );
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    protected synchronized void performScheduledJournalDeduplication() {
        log.info("Deduplication of journals started.");
        deduplicationSuggestionRepository.deleteByEntityType(IndexType.JOURNAL.name());

        int pageNumber = 0;
        int chunkSize = 20;
        boolean hasNextPage = true;
        var duplicatesFound = new ArrayList<Integer>();

        while (hasNextPage) {
            List<JournalIndex> chunk =
                journalIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(journal -> {
                if (duplicatesFound.contains(journal.getDatabaseId())) {
                    return;
                }

                var deduplicationQuery = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_sr").query(journal.getTitleSr())));
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_other").query(journal.getTitleOther())));
                            return eq;
                        });
                        return bq;
                    });
                    b.mustNot(sb -> sb.match(
                        m -> m.field("databaseId").query(journal.getDatabaseId())));
                    return b;
                })));

                var similarJournals = journalSearchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    JournalIndex.class,
                    "journal"
                ).getContent();

                if (!similarJournals.isEmpty()) {
                    handleDuplicate(
                        journal, similarJournals, duplicatesFound, IndexType.JOURNAL,
                        JournalIndex::getDatabaseId, JournalIndex::getTitleSr,
                        JournalIndex::getTitleOther, null
                    );
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    protected synchronized void performScheduledEventDeduplication() {
        log.info("Deduplication of events started.");
        deduplicationSuggestionRepository.deleteByEntityType(IndexType.EVENT.name());

        int pageNumber = 0;
        int chunkSize = 20;
        boolean hasNextPage = true;
        var duplicatesFound = new ArrayList<Integer>();

        while (hasNextPage) {
            List<EventIndex> chunk =
                eventIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(event -> {
                if (duplicatesFound.contains(event.getDatabaseId())) {
                    return;
                }

                var deduplicationQuery = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_sr").query(event.getNameSr())));
                            eq.should(sb -> sb.matchPhrase(
                                m -> m.field("title_other").query(event.getNameOther())));
                            return eq;
                        });
                        return bq;
                    });
                    b.mustNot(sb -> sb.match(
                        m -> m.field("databaseId").query(event.getDatabaseId())));
                    return b;
                })));

                var similarEvents = eventSearchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    EventIndex.class,
                    "events"
                ).getContent();

                if (!similarEvents.isEmpty()) {
                    handleDuplicate(
                        event, similarEvents, duplicatesFound, IndexType.EVENT,
                        EventIndex::getDatabaseId, EventIndex::getNameSr,
                        EventIndex::getNameOther, null
                    );
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    protected synchronized void performScheduledPersonDeduplication() {
        log.info("Deduplication of persons started.");
        deduplicationSuggestionRepository.deleteByEntityType(IndexType.PERSON.name());

        int pageNumber = 0;
        int chunkSize = 20;
        boolean hasNextPage = true;
        var duplicatesFound = new ArrayList<Integer>();

        while (hasNextPage) {
            List<PersonIndex> chunk =
                personIndexRepository.findAll(PageRequest.of(pageNumber, chunkSize)).getContent();

            chunk.forEach(person -> {
                if (duplicatesFound.contains(person.getDatabaseId())) {
                    return;
                }

                var tokens = List.of(person.getName().trim().split(" "));
                var minShouldMatch = (int) Math.ceil(tokens.size() * 0.6);

                var deduplicationQuery = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
                    b.must(bq -> {
                        bq.bool(eq -> {
                            tokens.forEach(
                                token -> {
                                    eq.should(sb -> sb.match(m -> m.field("name").query(token)));
                                });
                            eq.should(sb -> sb.match(
                                m -> m.field("birthdate").query(person.getBirthdate())));
                            return eq.minimumShouldMatch(Integer.toString(minShouldMatch));
                        });
                        return bq;
                    });
                    b.mustNot(sb -> sb.match(
                        m -> m.field("databaseId").query(person.getDatabaseId())));
                    return b;
                })));

                var similarPersons = personSearchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    PersonIndex.class,
                    "person"
                ).getContent();

                if (!similarPersons.isEmpty()) {
                    handleDuplicate(
                        person, similarPersons, duplicatesFound, IndexType.PERSON,
                        PersonIndex::getDatabaseId, PersonIndex::getName,
                        PersonIndex::getName, null
                    );
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }
    }

    private <T> void handleDuplicate(
        T entity,
        List<T> similarEntities,
        ArrayList<Integer> foundDuplicates,
        IndexType indexType,
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

            foundDuplicates.add(getIdFunction.apply(similarEntity));
            log.info("Found potential duplicate: {} ({}) == {} ({})",
                getTitleSrFunction.apply(entity),
                getTitleOtherFunction.apply(entity),
                getTitleSrFunction.apply(similarEntity),
                getTitleOtherFunction.apply(similarEntity));

            currentSessionCounter++;
            deduplicationSuggestionRepository.save(
                new DeduplicationSuggestion(
                    getIdFunction.apply(entity),
                    getIdFunction.apply(similarEntity),
                    getTitleSrFunction.apply(entity),
                    getTitleOtherFunction.apply(entity),
                    getTitleSrFunction.apply(similarEntity),
                    getTitleOtherFunction.apply(similarEntity),
                    indexType == IndexType.PUBLICATION ?
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
