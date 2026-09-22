package rs.teslaris.project.service.interfaces.commontypes;

import org.springframework.stereotype.Service;
import rs.teslaris.project.dto.funding.PrepopulatedFundingMetadataDTO;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;

@Service
public interface ProjectMetadataPrepopulationService {
    PrepopulatedProjectMetadataDTO fetchProjectDataForDoi(String doi);
}
