package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ProceedingsResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.ProceedingsService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class ProceedingsRevisionRestorer implements RevisionRestorer<ProceedingsResponseDTO> {

    private final ProceedingsService proceedingsService;


    @Override
    public String entityType() {
        return DocumentPublicationType.PROCEEDINGS.name();
    }

    @Override
    public Class<ProceedingsResponseDTO> dtoClass() {
        return ProceedingsResponseDTO.class;
    }

    @Override
    public void restore(Integer entityId, ProceedingsResponseDTO dto) {
        proceedingsService.updateProceedings(entityId, dto);
    }
}
