package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ThesisResponseDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.ThesisService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class ThesisRevisionRestorer implements RevisionRestorer<ThesisResponseDTO> {

    private final ThesisService thesisService;


    @Override
    public String entityType() {
        return DocumentPublicationType.THESIS.name();
    }

    @Override
    public Class<ThesisResponseDTO> dtoClass() {
        return ThesisResponseDTO.class;
    }

    @Override
    public void restore(Integer entityId, ThesisResponseDTO dto) {
        thesisService.editThesis(entityId, dto);
    }
}
