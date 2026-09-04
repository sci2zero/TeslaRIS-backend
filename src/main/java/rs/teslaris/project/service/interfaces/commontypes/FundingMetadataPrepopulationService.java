package rs.teslaris.project.service.interfaces.commontypes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;

@Service
public interface FundingMetadataPrepopulationService {

    PrepopulatedFundingMetadataDTO fetchFundingDataForDoi(String doi);

    PrepopulatedFundingMetadataDTO mapCrossrefFundingData(@Nullable JsonNode message, String doi);
}
