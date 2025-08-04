package rs.teslaris.core.indexrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.PersonIndex;

@Repository
public interface PersonIndexRepository extends ElasticsearchRepository<PersonIndex, String> {

    Optional<PersonIndex> findByDatabaseId(Integer databaseId);

    Page<PersonIndex> findByDatabaseIdIn(List<Integer> databaseIds, Pageable pageable);

    @Query("""
        {
          "bool": {
            "should": [
              { "term": { "scopus_author_id": "?0" }},
              { "term": { "open_alex_id": "?0" }},
              { "term": { "web_of_science_id": "?0" }}
            ]
          }
        }
        """)
    Optional<PersonIndex> findByScopusAuthorIdOrOpenAlexIdOrWebOfScienceId(String importId);

    long count();
}
