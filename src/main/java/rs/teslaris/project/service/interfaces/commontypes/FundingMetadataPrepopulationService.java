package rs.teslaris.project.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;

@Service
public interface FundingMetadataPrepopulationService {
    PrepopulatedFundingMetadataDTO fetchFundingDataForDoi(String doi);
}
