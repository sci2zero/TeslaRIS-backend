package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.core.service.interfaces.JPAService;
import rs.teslaris.project.dto.funding.FundingPartDTO;
import rs.teslaris.project.model.funding.FundingPart;

@Service
public interface FundingPartService extends JPAService<FundingPart> {

    FundingPart createFundingPart(FundingPartDTO fundingPartDTO);

    void updateFundingPart(Integer fundingPartId, FundingPartDTO fundingPartDTO);

    void deleteFundingPart(Integer fundingPartId);
}
