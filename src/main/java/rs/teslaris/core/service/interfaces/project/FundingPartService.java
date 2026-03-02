package rs.teslaris.core.service.interfaces.project;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.project.FundingPartDTO;
import rs.teslaris.core.model.project.FundingPart;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface FundingPartService extends JPAService<FundingPart> {

    FundingPart createFundingPart(FundingPartDTO fundingPartDTO);

    void updateFundingPart(Integer fundingPartId, FundingPartDTO fundingPartDTO);

    void deleteFundingPart(Integer fundingPartId);
}
