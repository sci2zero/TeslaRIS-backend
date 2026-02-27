package rs.teslaris.core.indexrepository.project;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.core.indexmodel.project.FundingCallIndex;

public interface FundingCallIndexRepository
    extends ElasticsearchRepository<FundingCallIndex, String> {

    Optional<FundingCallIndex> findFundingCallIndexByDatabaseId(Integer databaseId);
}
