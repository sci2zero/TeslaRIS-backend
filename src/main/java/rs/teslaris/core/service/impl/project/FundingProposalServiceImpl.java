package rs.teslaris.core.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.project.FundingProposal;
import rs.teslaris.core.repository.project.FundingProposalRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.project.FundingProposalService;

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
