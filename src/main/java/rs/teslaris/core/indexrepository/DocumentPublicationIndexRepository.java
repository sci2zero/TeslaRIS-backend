package rs.teslaris.core.indexrepository;


import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Repository
public interface DocumentPublicationIndexRepository extends
    ElasticsearchRepository<DocumentPublicationIndex, String> {

    Optional<DocumentPublicationIndex> findDocumentPublicationIndexByDatabaseId(Integer databaseId);

    long count();

    Page<DocumentPublicationIndex> findByTypeAndEventId(String type, Integer eventId,
                                                        Pageable pageable);

    @Query("{\"bool\": " +
        "{\"must\": [" +
        "{\"term\": {\"type\": \"?0\"}}, " +
        "{\"term\": {\"journal_id\": \"?1\"}}, " +
        "{\"terms\": {\"author_ids\": [\"?2\"]}}" +
        "]}}")
    List<DocumentPublicationIndex> findByTypeAndJournalIdAndAuthorIds(String type,
                                                                      Integer journalId,
                                                                      Integer authorId);

    @Query("{\"bool\": " +
        "{\"must\": [" +
        "{\"terms\": {\"author_ids\": [\"?0\"]}}" +
        "]}}")
    Page<DocumentPublicationIndex> findByAuthorIds(Integer authorId, Pageable pageable);

    @Query("{\"bool\": " +
        "{\"must\": [" +
        "{\"term\": {\"type\": \"?0\"}}, " +
        "{\"term\": {\"journal_id\": \"?1\"}}, " +
        "]}}")
    Page<DocumentPublicationIndex> findByTypeAndJournalId(String type, Integer journalId,
                                                          Pageable pageable);

    @Query("{\"bool\": " +
        "{\"must\": [" +
        "{\"term\": {\"publisher_id\": \"?0\"}}, " +
        "]}}")
    Page<DocumentPublicationIndex> findByPublisherId(Integer publisherId, Pageable pageable);
}
