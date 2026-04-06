package rs.teslaris.project.indexrepository.funding;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.project.indexmodel.funding.FundingCallIndex;

public interface FundingCallIndexRepository
    extends ElasticsearchRepository<FundingCallIndex, String> {

    Optional<FundingCallIndex> findFundingCallIndexByDatabaseId(Integer databaseId);
}
