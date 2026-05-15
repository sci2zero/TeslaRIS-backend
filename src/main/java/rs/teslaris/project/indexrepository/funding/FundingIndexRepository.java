package rs.teslaris.project.indexrepository.funding;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import rs.teslaris.project.indexmodel.funding.FundingCallIndex;
import rs.teslaris.project.indexmodel.funding.FundingIndex;

import java.util.Optional;

public interface FundingIndexRepository extends ElasticsearchRepository<FundingIndex, String> {
    Optional<FundingIndex> findFundingIndexByDatabaseId(Integer databaseId);
}
