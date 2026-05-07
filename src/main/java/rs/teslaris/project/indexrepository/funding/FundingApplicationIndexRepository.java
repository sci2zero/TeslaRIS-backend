package rs.teslaris.project.indexrepository.funding;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.indexmodel.funding.FundingApplicationIndex;

import java.util.Optional;

@Repository
public interface FundingApplicationIndexRepository
        extends ElasticsearchRepository<FundingApplicationIndex, String> {

    Optional<FundingApplicationIndex> findFundingApplicationIndexByDatabaseId(Integer databaseId);
}
