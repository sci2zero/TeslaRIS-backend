package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.indexmodel.PrizeIndex;

public interface PrizeIndexRepository extends ElasticsearchRepository<PrizeIndex, String> {

    Optional<PrizeIndex> findPrizeIndexByDatabaseId(Integer databaseId);

    Page<DocumentPublicationIndex> findByPersonId(Integer personId, Pageable pageable);
}
