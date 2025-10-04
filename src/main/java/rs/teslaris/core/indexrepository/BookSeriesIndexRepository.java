package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.BookSeriesIndex;

@Repository
public interface BookSeriesIndexRepository
    extends ElasticsearchRepository<BookSeriesIndex, String> {

    Optional<BookSeriesIndex> findBookSeriesIndexByDatabaseId(Integer databaseId);

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
    Optional<BookSeriesIndex> findBookSeriesIndexByeISSNOrPrintISSN(String eISSN, String printISSN);
}
