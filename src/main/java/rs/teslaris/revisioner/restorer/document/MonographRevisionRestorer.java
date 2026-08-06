package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.MonographService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class MonographRevisionRestorer implements RevisionRestorer<MonographDTO> {

    private final MonographService monographService;


    @Override
    public String entityType() {
        return DocumentPublicationType.MONOGRAPH.name();
    }

    @Override
    public Class<MonographDTO> dtoClass() {
        return MonographDTO.class;
    }

    @Override
    public void restore(Integer entityId, MonographDTO dto) {
        monographService.editMonograph(entityId, dto);
    }
}
