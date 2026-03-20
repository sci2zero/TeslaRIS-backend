package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.model.funding.FundingApplication;

@Service
public interface FundingApplicationService extends JPAService<FundingApplication> {
}
