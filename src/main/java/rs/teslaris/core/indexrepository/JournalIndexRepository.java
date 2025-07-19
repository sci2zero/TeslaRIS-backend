package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.JournalIndex;

@Repository
public interface JournalIndexRepository extends ElasticsearchRepository<JournalIndex, String> {

    Optional<JournalIndex> findJournalIndexByDatabaseId(Integer databaseId);

    @Query("""
        {
          "bool": {
            "should": [
              { "term": { "e_issn": "?0" }},
              { "term": { "e_issn": "?1" }},
              { "term": { "print_issn": "?0" }},
              { "term": { "print_issn": "?1" }}
            ]
          }
        }
        """)
    Optional<JournalIndex> findJournalIndexByeISSNOrPrintISSN(String eISSN, String printISSN);

    @Query("""
        {
          "bool": {
            "should": [
              { "term": { "e_issn": "?0" }},
              { "term": { "e_issn": "?1" }},
              { "term": { "print_issn": "?0" }},
              { "term": { "print_issn": "?1" }},
              { "term": { "open_alex_id": "?2" }}
            ]
          }
        }
        """)
    Optional<JournalIndex> findByAnyIdentifiers(String eISSN, String printISSN, String openALexId);
}
