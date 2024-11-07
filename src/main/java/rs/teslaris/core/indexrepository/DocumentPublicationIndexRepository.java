package rs.teslaris.core.indexrepository;


import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Repository
public interface DocumentPublicationIndexRepository extends
    ElasticsearchRepository<DocumentPublicationIndex, String> {

    Optional<DocumentPublicationIndex> findDocumentPublicationIndexByDatabaseId(Integer databaseId);

    long count();

    Page<DocumentPublicationIndex> findByTypeAndEventId(String type, Integer eventId,
                                                        Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndProceedingsId(String type, Integer proceedingsId,
                                                              Pageable pageable);

    List<DocumentPublicationIndex> findByTypeAndJournalIdAndAuthorIds(String type,
                                                                      Integer journalId,
                                                                      Integer authorId);

    List<DocumentPublicationIndex> findByTypeAndMonographIdAndAuthorIds(String type,
                                                                        Integer monographId,
                                                                        Integer authorId);

    Page<DocumentPublicationIndex> findByAuthorIds(Integer authorId, Pageable pageable);

    Page<DocumentPublicationIndex> findByOrganisationUnitIdsIn(List<Integer> organisationUnitIds,
                                                               Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndJournalId(String type, Integer journalId,
                                                          Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeInAndPublicationSeriesId(List<String> types,
                                                                      Integer publicationSeriesId,
                                                                      Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeAndMonographId(String type, Integer monographId,
                                                            Pageable pageable);

    Page<DocumentPublicationIndex> findByPublisherId(Integer publisherId, Pageable pageable);

    Page<DocumentPublicationIndex> findByTypeIn(List<String> types, Pageable pageable);

    void deleteByJournalId(Integer journalId);
}
