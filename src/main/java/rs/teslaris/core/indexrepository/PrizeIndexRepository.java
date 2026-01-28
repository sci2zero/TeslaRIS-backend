package rs.teslaris.core.indexrepository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PrizeIndex;

public interface PrizeIndexRepository extends ElasticsearchRepository<PrizeIndex, String> {

    Optional<PrizeIndex> findPrizeIndexByDatabaseId(Integer databaseId);

    Page<DocumentPublicationIndex> findByPersonId(Integer personId, Pageable pageable);

    @Query("""
        {
            "bool": {
                "must": [
                    {"term": {"person_id": ?0}},
                    {"range": {"date_of_acquisition": {"gte": "?1", "lte": "?2"}}},
                    {"exists": {"field": "assessed_by"}}
                ]
            }
        }
        """)
    Page<PrizeIndex> findByPersonIdAndDateBetween(Integer authorId, LocalDate startDate,
                                                  LocalDate endDate, Pageable pageable);
}
