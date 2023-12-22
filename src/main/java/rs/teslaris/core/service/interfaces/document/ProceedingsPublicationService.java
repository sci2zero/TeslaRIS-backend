package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.ProceedingsPublication;

@Service
public interface ProceedingsPublicationService {

    ProceedingsPublicationDTO readProceedingsPublicationById(Integer proceedingsId);

    List<ProceedingsPublicationResponseDTO> findAuthorsProceedingsForEvent(Integer eventId,
                                                                           Integer authorId);

    ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO);

    void editProceedingsPublication(Integer publicationId,
                                    ProceedingsPublicationDTO publicationDTO);

    void deleteProceedingsPublication(Integer proceedingsPublicationId);

    void indexProceedingsPublication(ProceedingsPublication publication,
                                     DocumentPublicationIndex index);
}
