package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.ProceedingsPublication;

@Service
public interface ProceedingsPublicationService {

    ProceedingsPublicationDTO readProceedingsPublicationById(Integer proceedingsId);

    ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO);

    void editProceedingsPublication(Integer publicationId,
                                    ProceedingsPublicationDTO publicationDTO);

    void deleteProceedingsPublication(Integer proceedingsPublicationId);

    void indexProceedingsPublication(ProceedingsPublication publication,
                                     DocumentPublicationIndex index);
}
