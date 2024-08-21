package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ThesisDTO;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.model.document.Thesis;

@Service
public interface ThesisService {

    ThesisResponseDTO readThesisById(Integer thesisId);

    Thesis createThesis(ThesisDTO thesisDTO, Boolean index);

    void editThesis(Integer thesisId, ThesisDTO thesisDTO);

    void deleteThesis(Integer thesisId);

    void reindexTheses();
}
