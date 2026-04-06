package rs.teslaris.project.indexrepository.funding;

import java.util.Optional;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.indexmodel.funding.FundingProgramIndex;

@Repository
public interface FundingProgramIndexRepository
    extends ElasticsearchRepository<FundingProgramIndex, String> {

    Optional<FundingProgramIndex> findFundingProgramIndexByDatabaseId(Integer databaseId);
}
