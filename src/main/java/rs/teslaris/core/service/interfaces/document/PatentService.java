package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.Patent;

@Service
public interface PatentService {

    PatentDTO readPatentById(Integer patentID);

    Patent createPatent(PatentDTO patentDTO, Boolean index);

    void editPatent(Integer patentId, PatentDTO patentDTO);

    void deletePatent(Integer patentId);

    void reindexPatents();
}
