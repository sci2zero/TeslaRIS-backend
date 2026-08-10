package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.IntellectualPropertyDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.IntellectualPropertyService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class IntellectualPropertyRevisionRestorer
    implements RevisionRestorer<IntellectualPropertyDTO> {

    private final IntellectualPropertyService intellectualPropertyService;


    @Override
    public String entityType() {
        return DocumentPublicationType.INTELLECTUAL_PROPERTY.name();
    }

    @Override
    public Class<IntellectualPropertyDTO> dtoClass() {
        return IntellectualPropertyDTO.class;
    }

    @Override
    public void restore(Integer entityId, IntellectualPropertyDTO dto) {
        intellectualPropertyService.editIntellectualProperty(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return intellectualPropertyService.readIntellectualPropertyById(entityId);
    }
}
