package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.dto.document.ProceedingsPublicationResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationIndex;
import rs.teslaris.core.model.document.ProceedingsPublication;

@Service
public interface ProceedingsPublicationService {

    ProceedingsPublicationDTO readProceedingsPublicationById(Integer publicationId);

    List<ProceedingsPublicationResponseDTO> findAuthorsProceedingsForEvent(Integer eventId,
                                                                           Integer authorId);

    Page<DocumentPublicationIndex> findPublicationsInProceedings(Integer proceedingsId,
                                                                 Pageable pageable);

    ProceedingsPublication createProceedingsPublication(
        ProceedingsPublicationDTO proceedingsPublicationDTO, Boolean index);

    void editProceedingsPublication(Integer publicationId,
                                    ProceedingsPublicationDTO publicationDTO);

    void deleteProceedingsPublication(Integer proceedingsPublicationId);

    Page<DocumentPublicationIndex> findProceedingsForEvent(Integer eventId, Pageable pageable);

    void reindexProceedingsPublications();

    void indexProceedingsPublication(ProceedingsPublication publication,
                                     DocumentPublicationIndex index);
}
