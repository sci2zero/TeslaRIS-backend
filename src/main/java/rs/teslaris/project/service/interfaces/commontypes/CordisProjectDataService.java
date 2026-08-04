package rs.teslaris.project.service.interfaces.commontypes;

import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;

public interface CordisProjectDataService {

    PrepopulatedProjectMetadataDTO fetchFullMetadata(String cordisProjectId, String doi);
}