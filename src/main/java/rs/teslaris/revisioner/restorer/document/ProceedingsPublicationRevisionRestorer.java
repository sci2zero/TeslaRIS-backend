package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.ProceedingsPublicationService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class ProceedingsPublicationRevisionRestorer
    implements RevisionRestorer<ProceedingsPublicationDTO> {

    private final ProceedingsPublicationService proceedingsPublicationService;


    @Override
    public String entityType() {
        return DocumentPublicationType.PROCEEDINGS_PUBLICATION.name();
    }

    @Override
    public Class<ProceedingsPublicationDTO> dtoClass() {
        return ProceedingsPublicationDTO.class;
    }

    @Override
    public void restore(Integer entityId, ProceedingsPublicationDTO dto) {
        proceedingsPublicationService.editProceedingsPublication(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return proceedingsPublicationService.readProceedingsPublicationById(entityId);
    }
}
