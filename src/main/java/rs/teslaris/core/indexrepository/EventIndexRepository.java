package rs.teslaris.core.indexrepository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.EventIndex;
import rs.teslaris.core.indexmodel.EventType;

@Repository
public interface EventIndexRepository extends ElasticsearchRepository<EventIndex, String> {

    Optional<EventIndex> findByDatabaseId(Integer databaseId);

    Optional<EventIndex> findByEventTypeAndDatabaseId(EventType eventType, Integer databaseId);

    Long countByRelatedInstitutionIds(Integer institutionId);

    Long countByClassifiedBy(Integer classifiedBy);

    Long countByRelatedInstitutionIdsAndClassifiedBy(Integer institutionId, Integer classifiedBy);

    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"related_person_ids": ?0}},
                    {"range": {"date_sortable": {"gte": "?1", "lte": "?2"}}},
                    {"exists": {"field": "classified_by"}}
                ]
            }
        }
        """)
    Page<EventIndex> findByPersonIdAndDateBetween(Integer authorId, LocalDate startDate,
                                                  LocalDate endDate, Pageable pageable);
}
