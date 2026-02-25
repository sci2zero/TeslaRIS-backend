package rs.teslaris.core.repository.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rs.teslaris.core.model.project.FundingProposal;

@Repository
public interface FundingProposalRepository extends JpaRepository<FundingProposal, Integer> {
}
