package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingApplication;

@Repository
public interface FundingApplicationRepository extends JpaRepository<FundingApplication, Integer> {
}
