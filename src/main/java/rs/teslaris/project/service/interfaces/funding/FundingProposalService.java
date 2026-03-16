package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.model.funding.FundingProposal;

@Service
public interface FundingProposalService extends JPAService<FundingProposal> {
}
