package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.teslaris.core.converter.commontypes.DocumentDeduplicationSuggestionConverter;
import rs.teslaris.core.dto.commontypes.DocumentDeduplicationSuggestionDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationBlacklist;
import rs.teslaris.core.model.commontypes.DocumentDeduplicationSuggestion;
import rs.teslaris.core.repository.commontypes.DocumentDeduplicationBlacklistRepository;
import rs.teslaris.core.repository.commontypes.DocumentDeduplicationSuggestionRepository;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;
import rs.teslaris.core.service.interfaces.document.DocumentPublicationService;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeduplicationServiceImpl implements DeduplicationService {

    private static volatile boolean deduplicationLock = false;
    private static Integer currentSessionCounter = 0;

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final SearchService<DocumentPublicationIndex> searchService;

    private final DocumentDeduplicationSuggestionRepository deduplicationSuggestionRepository;

    private final DocumentPublicationService documentPublicationService;

    private final DocumentDeduplicationBlacklistRepository documentDeduplicationBlacklistRepository;


    @Override
    public boolean startDocumentDeduplicationProcessBeforeSchedule() {
        log.info("Trying to start deduplication ahead of time.");
        if (deduplicationLock) {
            return false;
        }

        startDeduplicationAsync();
        return true;
    }

    @Override
    public void deleteDocumentSuggestion(Integer suggestionId) {
        deduplicationSuggestionRepository.delete(
            findDocumentDeduplicationSuggestionById(suggestionId));
    }

    @Override
    public void flagDocumentAsNotDuplicate(Integer suggestionId) {
        var suggestion = findDocumentDeduplicationSuggestionById(suggestionId);

        var blacklistEntry =
            documentDeduplicationBlacklistRepository.findByLeftDocumentIdAndRightDocumentId(
                suggestion.getLeftDocument().getId(), suggestion.getRightDocument().getId());

        if (blacklistEntry.isEmpty()) {
            documentDeduplicationBlacklistRepository.save(
                new DocumentDeduplicationBlacklist(suggestion.getLeftDocument().getId(),
                    suggestion.getRightDocument().getId()));
        }

        deleteDocumentSuggestion(suggestionId);
    }

    @Override
    public Page<DocumentDeduplicationSuggestionDTO> getDeduplicationSuggestions(Pageable pageable) {
        var suggestionsPage = deduplicationSuggestionRepository.findAll(pageable);

        var dtoList = suggestionsPage
            .stream()
            .map(DocumentDeduplicationSuggestionConverter::toDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, suggestionsPage.getTotalElements());
    }

    @Async
    private void startDeduplicationAsync() {
        try {
            performScheduledDocumentDeduplication();
        } finally {
            deduplicationLock = false;
        }
    }

    @Scheduled(cron = "${deduplication.schedule}")
    protected void performScheduledDocumentDeduplication() {
        if (deduplicationLock) {
            log.info(
                "Deduplication of publications startup aborted due to process already running.");
            return;
        }

        deduplicationLock = true;
        log.info("Deduplication of publications started.");
        deduplicationSuggestionRepository.deleteAll(); // TODO: Should we clean everything or check for duplicate suggestions at the time of saving?

        int pageNumber = 0;
        int chunkSize = 20;
        boolean hasNextPage = true;
        var duplicatesFound = new ArrayList<Integer>();

        while (hasNextPage) {
            List<DocumentPublicationIndex> chunk = documentPublicationIndexRepository.findByTypeIn(
                List.of(DocumentPublicationType.MONOGRAPH.name(),
                    DocumentPublicationType.PROCEEDINGS.name(),
                    DocumentPublicationType.PROCEEDINGS_PUBLICATION.name(),
                    DocumentPublicationType.JOURNAL_PUBLICATION.name(),
                    DocumentPublicationType.PATENT.name(), DocumentPublicationType.SOFTWARE.name(),
                    DocumentPublicationType.DATASET.name()),
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
                        m -> m.field("database_id").query(publication.getDatabaseId())));
                    return b;
                })));

                var similarPublications = searchService.runQuery(
                    deduplicationQuery._toQuery(),
                    PageRequest.of(0, 2),
                    DocumentPublicationIndex.class,
                    "document_publication"
                ).getContent();

                if (!similarPublications.isEmpty()) {
                    handleDuplicate(publication, similarPublications, duplicatesFound);
                }
            });

            pageNumber++;
            hasNextPage = chunk.size() == chunkSize;
        }

        log.info("Deduplication of publications process completed. Total suggestions found: {}.",
            currentSessionCounter);
        currentSessionCounter = 0;
        deduplicationLock = false;
    }

    private void handleDuplicate(DocumentPublicationIndex publication,
                                 List<DocumentPublicationIndex> similarPublications,
                                 ArrayList<Integer> foundDuplicates) {
        for (var similarPublication : similarPublications) {
            var leftDocument = documentPublicationService.findDocumentById(
                publication.getDatabaseId());
            var rightDocument =
                documentPublicationService.findDocumentById(similarPublication.getDatabaseId());

            var blacklistEntry =
                documentDeduplicationBlacklistRepository.findByLeftDocumentIdAndRightDocumentId(
                    leftDocument.getId(), rightDocument.getId());

            if (blacklistEntry.isPresent()) {
                continue;
            }

            foundDuplicates.add(similarPublication.getDatabaseId());
            log.info("Found potential publication duplicate: {} ({}) == {} ({})",
                publication.getTitleSr(),
                publication.getTitleOther(), similarPublication.getTitleSr(),
                similarPublication.getTitleOther());

            currentSessionCounter++;
            deduplicationSuggestionRepository.save(
                new DocumentDeduplicationSuggestion(leftDocument, rightDocument,
                    DocumentPublicationType.valueOf(publication.getType())));
        }
    }

    private DocumentDeduplicationSuggestion findDocumentDeduplicationSuggestionById(
        Integer suggestionId) {
        return deduplicationSuggestionRepository.findById(suggestionId)
            .orElseThrow(() -> new NotFoundException("Suggestion with given ID does not exist."));
    }
}
