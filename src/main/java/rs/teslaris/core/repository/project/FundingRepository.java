package rs.teslaris.core.repository.project;

import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.Funding;
import rs.teslaris.core.repository.JPASoftDeleteRepository;

@Repository
public interface FundingRepository extends JPASoftDeleteRepository<Funding> {
}
