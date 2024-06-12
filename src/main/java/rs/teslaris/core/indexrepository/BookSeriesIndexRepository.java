package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.BookSeriesIndex;

@Repository
public interface BookSeriesIndexRepository
    extends ElasticsearchRepository<BookSeriesIndex, String> {

    Optional<BookSeriesIndex> findBookSeriesIndexByDatabaseId(Integer databaseId);
}
