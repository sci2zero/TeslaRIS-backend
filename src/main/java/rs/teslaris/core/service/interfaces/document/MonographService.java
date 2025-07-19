package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.Monograph;

@Service
public interface MonographService {

    Monograph findMonographById(Integer monographId);

    Monograph findRaw(Integer monographId);

    Page<DocumentPublicationIndex> searchMonographs(List<String> tokens, boolean onlyBooks);

    MonographDTO readMonographById(Integer monographId);

    Monograph createMonograph(MonographDTO monographDTO, Boolean index);

    void editMonograph(Integer monographId, MonographDTO monographDTO);

    void deleteMonograph(Integer monographId);

    void forceDeleteMonograph(Integer monographId);

    void reindexMonographs();

    void indexMonograph(Monograph monograph, DocumentPublicationIndex index);

    void indexMonograph(Monograph monograph);

    boolean isIdentifierInUse(String identifier, Integer monographId);
}
