package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.SoftwareDTO;
import rs.teslaris.core.model.document.Software;

@Service
public interface SoftwareService {

    Software findSoftwareById(Integer softwareId);

    SoftwareDTO readSoftwareById(Integer softwareId);

    Software createSoftware(SoftwareDTO softwareDTO, Boolean index);

    void editSoftware(Integer softwareId, SoftwareDTO softwareDTO);

    void deleteSoftware(Integer softwareId);

    void reindexSoftware();

    void indexSoftware(Software software);
}
