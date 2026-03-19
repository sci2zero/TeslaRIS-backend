package rs.teslaris.project.service.interfaces.funding;

import org.springframework.stereotype.Service;
import rs.teslaris.project.dto.funding.FundingCallDTO;
import rs.teslaris.project.model.funding.FundingCall;

@Service
public interface PersonFundingCallContributionService {

    void setPersonFundingContributionsForFundingCall(FundingCall fundingCall,
                                                     FundingCallDTO fundingCallDTO);
}
