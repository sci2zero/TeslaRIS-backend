package rs.teslaris.project.service.interfaces.commontypes;

import jakarta.annotation.Nullable;
import org.w3c.dom.Document;
import rs.teslaris.project.dto.project.PrepopulatedProjectMetadataDTO;

public interface CordisProjectDataService {

    PrepopulatedProjectMetadataDTO fetchMetadata(String cordisProjectId, String doi);

    PrepopulatedProjectMetadataDTO mapProjectMetadata(@Nullable Document document, String doi);
}
