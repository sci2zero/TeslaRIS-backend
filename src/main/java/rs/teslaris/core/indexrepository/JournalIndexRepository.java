package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.JournalIndex;

@Repository
public interface JournalIndexRepository extends ElasticsearchRepository<JournalIndex, String> {

    Optional<JournalIndex> findJournalIndexByDatabaseId(Integer databaseId);

    Optional<JournalIndex> findJournalIndexByeISSNOrPrintISSN(String eISSN, String printISSN);
}
