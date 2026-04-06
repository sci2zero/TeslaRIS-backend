package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.funding.Funding;
import rs.teslaris.project.repository.funding.FundingRepository;
import rs.teslaris.project.service.interfaces.funding.FundingService;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl extends JPAServiceImpl<Funding> implements FundingService {

    private final FundingRepository fundingRepository;

    @Override
    protected JpaRepository<Funding, Integer> getEntityRepository() {
        return fundingRepository;
    }
}
