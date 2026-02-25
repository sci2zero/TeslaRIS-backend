package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PatentDTO;
import rs.teslaris.core.model.document.Patent;

@Service
public interface PatentService {

    Patent findPatentById(Integer patentId);

    PatentDTO readPatentById(Integer patentId);

    Patent createPatent(PatentDTO patentDTO, Boolean index);

    void editPatent(Integer patentId, PatentDTO patentDTO);

    void deletePatent(Integer patentId);

    void reindexPatents();

    void indexPatent(Patent patent);

    PatentDTO readPatentByOldId(Integer oldId);
}
