package rs.teslaris.project.repository.funding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.project.model.funding.FundingProposal;

@Repository
public interface FundingProposalRepository extends JpaRepository<FundingProposal, Integer> {
}
