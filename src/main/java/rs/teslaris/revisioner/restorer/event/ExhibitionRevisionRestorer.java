package rs.teslaris.revisioner.restorer.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.ExhibitionDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.ExhibitionService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class ExhibitionRevisionRestorer implements RevisionRestorer<ExhibitionDTO> {

    private final ExhibitionService exhibitionService;


    @Override
    public String entityType() {
        return EntityType.EXHIBITION.name();
    }

    @Override
    public Class<ExhibitionDTO> dtoClass() {
        return ExhibitionDTO.class;
    }

    @Override
    public void restore(Integer entityId, ExhibitionDTO dto) {
        exhibitionService.updateExhibition(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return exhibitionService.readExhibition(entityId);
    }
}
