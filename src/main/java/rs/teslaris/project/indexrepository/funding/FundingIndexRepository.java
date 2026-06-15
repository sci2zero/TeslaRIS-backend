package rs.teslaris.project.indexrepository.funding;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.project.indexmodel.funding.FundingIndex;

public interface FundingIndexRepository extends ElasticsearchRepository<FundingIndex, String> {
    Optional<FundingIndex> findFundingIndexByDatabaseId(Integer databaseId);
}
