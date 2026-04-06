package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.model.funding.Funding;

@Service
public interface FundingService extends JPAService<Funding> {
}
