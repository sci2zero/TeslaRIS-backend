package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PrepopulatedMetadataDTO;

@Service
public interface MetadataPrepopulationService {

    PrepopulatedMetadataDTO fetchBibTexDataForDoi(String doi, Integer importPersonId);
}
