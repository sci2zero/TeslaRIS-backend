package rs.teslaris.project.service.impl.funding;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.project.model.funding.FundingApplication;
import rs.teslaris.project.repository.funding.FundingApplicationRepository;
import rs.teslaris.project.service.interfaces.funding.FundingApplicationService;

@Service
@RequiredArgsConstructor
public class FundingApplicationServiceImpl extends JPAServiceImpl<FundingApplication>
    implements FundingApplicationService {

    private final FundingApplicationRepository fundingApplicationRepository;

    @Override
    protected JpaRepository<FundingApplication, Integer> getEntityRepository() {
        return fundingApplicationRepository;
    }
}
