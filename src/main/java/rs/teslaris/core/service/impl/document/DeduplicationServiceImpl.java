package rs.teslaris.core.service.impl.document;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.indexrepository.DocumentPublicationIndexRepository;
import rs.teslaris.core.service.interfaces.commontypes.SearchService;
import rs.teslaris.core.service.interfaces.document.DeduplicationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationServiceImpl implements DeduplicationService {

    private final DocumentPublicationIndexRepository documentPublicationIndexRepository;

    private final SearchService<DocumentPublicationIndex> searchService;

    public void startDeduplicationProcessBeforeSchedule() {
        log.info("Deduplication started ahead of time.");
        performScheduledDeduplication();
    }

    @Scheduled(cron = "${deduplication.schedule}")
    protected void performScheduledDeduplication() {
        log.info("Deduplication started.");

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

        log.info("Deduplication process completed.");
    }

    private void handleDuplicate(DocumentPublicationIndex publication,
                                 List<DocumentPublicationIndex> similarPublications,
                                 ArrayList<Integer> foundDuplicates) {
        for (var similarPublication : similarPublications) {
            foundDuplicates.add(similarPublication.getDatabaseId());
            System.out.println(similarPublication.getTitleSr());
        }
    }
}
