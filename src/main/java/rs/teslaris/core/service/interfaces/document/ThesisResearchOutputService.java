package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;

@Service
public interface ThesisResearchOutputService {

    Page<DocumentPublicationIndex> readResearchOutputsForThesis(Integer thesisId,
                                                                Pageable pageable);

    void addResearchOutput(Integer thesisId, Integer researchOutputId);

    void removeResearchOutput(Integer thesisId, Integer researchOutputId);
}
