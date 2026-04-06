package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingPart;

@Repository
public interface FundingPartRepository extends JpaRepository<FundingPart, Integer> {
}
