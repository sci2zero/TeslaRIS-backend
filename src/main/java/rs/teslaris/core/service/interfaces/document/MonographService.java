package rs.teslaris.core.service.interfaces.document;

import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.model.document.Monograph;

public interface MonographService {

    MonographDTO readMonographById(Integer monographId);

    Monograph createMonograph(MonographDTO monographDTO, Boolean index);

    void updateMonograph(Integer monographId, MonographDTO monographDTO);

    void deleteMonograph(Integer monographId);

    void reindexMonographs();
}
