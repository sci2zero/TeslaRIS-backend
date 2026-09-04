package rs.teslaris.project.service.interfaces.commontypes;

import jakarta.annotation.Nullable;
import org.w3c.dom.Document;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;

public interface CordisFundingDataService {

    PrepopulatedFundingMetadataDTO fetchMetadata(String cordisProjectId, String doi);

    /**
     * Maps an already fetched CORDIS document, so that prepopulating a project with its funding
     * costs a single CORDIS request instead of two.
     */
    PrepopulatedFundingMetadataDTO mapFundingMetadata(@Nullable Document document, String doi);
}
