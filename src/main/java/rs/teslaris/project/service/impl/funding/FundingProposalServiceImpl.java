package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.funding.FundingProposal;
import rs.teslaris.project.repository.funding.FundingProposalRepository;
import rs.teslaris.project.service.interfaces.funding.FundingProposalService;

@Service
@RequiredArgsConstructor
public class FundingProposalServiceImpl extends JPAServiceImpl<FundingProposal>
    implements FundingProposalService {

    private final FundingProposalRepository fundingProposalRepository;

    @Override
    protected JpaRepository<FundingProposal, Integer> getEntityRepository() {
        return fundingProposalRepository;
    }
}
