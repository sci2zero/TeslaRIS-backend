package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.model.document.ProceedingsPublication;

@Service
public interface ProceedingsPublicationService {

    ProceedingsPublicationDTO readProceedingsPublicationById(Integer proceedingsId);

    ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO);

    void editProceedingsPublication(Integer publicationId,
                                    ProceedingsPublicationDTO publicationDTO);

    void deleteProceedingsPublication(Integer proceedingsPublicationId);
}
