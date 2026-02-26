package rs.teslaris.core.indexrepository;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.indexmodel.FundingProgramIndex;

@Repository
public interface FundingProgramIndexRepository
    extends ElasticsearchRepository<FundingProgramIndex, String> {

    Optional<FundingProgramIndex> findFundingProgramIndexByDatabaseId(Integer databaseId);
}
