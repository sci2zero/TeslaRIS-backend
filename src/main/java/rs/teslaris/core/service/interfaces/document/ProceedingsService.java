package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsDTO;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.Proceedings;

@Service
public interface ProceedingsService {

    ProceedingsResponseDTO readProceedingsById(Integer proceedingsId);

    List<ProceedingsResponseDTO> readProceedingsForEventId(Integer eventId);

    Page<DocumentPublicationIndex> findProceedingsForBookSeries(Integer bookSeriesId,
                                                                Pageable pageable);

    Proceedings findProceedingsById(Integer proceedingsId);

    Proceedings createProceedings(ProceedingsDTO proceedingsDTO, boolean index);

    void updateProceedings(Integer proceedingsId, ProceedingsDTO proceedingsDTO);

    void deleteProceedings(Integer proceedingsId);

    void indexProceedings(Proceedings proceedings, DocumentPublicationIndex index);
}
