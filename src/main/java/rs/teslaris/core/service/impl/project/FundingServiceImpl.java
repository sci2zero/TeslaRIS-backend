package rs.teslaris.core.service.impl.project;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import rs.teslaris.core.model.project.Funding;
import rs.teslaris.core.repository.project.FundingRepository;
import rs.teslaris.core.service.impl.JPAServiceImpl;
import rs.teslaris.core.service.interfaces.project.FundingService;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl extends JPAServiceImpl<Funding> implements FundingService {

    private final FundingRepository fundingRepository;

    @Override
    protected JpaRepository<Funding, Integer> getEntityRepository() {
        return fundingRepository;
    }
}
