package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.model.document.IntellectualProperty;

@Service
public interface IntellectualPropertyService {

    IntellectualProperty findIntellectualPropertyById(Integer intellectualPropertyId);

    IntellectualPropertyDTO readIntellectualPropertyById(Integer intellectualPropertyId);

    IntellectualProperty createIntellectualProperty(IntellectualPropertyDTO intellectualPropertyDTO,
                                                    Boolean index);

    void editIntellectualProperty(Integer intellectualPropertyId,
                                  IntellectualPropertyDTO intellectualPropertyDTO);

    void deleteIntellectualProperty(Integer intellectualPropertyId);

    void reindexIntellectualProperties();

    void indexIntellectualProperty(IntellectualProperty intellectualProperty);

    IntellectualPropertyDTO readIntellectualPropertyByOldId(Integer oldId);
}
