package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.Funding;

@Repository
public interface FundingRepository extends JpaRepository<Funding, Integer> {
}
