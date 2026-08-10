package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.MonographPublicationDTO;
import rs.teslaris.core.indexmodel.DocumentPublicationType;
import rs.teslaris.core.service.interfaces.document.MonographPublicationService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class MonographPublicationRevisionRestorer
    implements RevisionRestorer<MonographPublicationDTO> {

    private final MonographPublicationService monographPublicationService;


    @Override
    public String entityType() {
        return DocumentPublicationType.MONOGRAPH_PUBLICATION.name();
    }

    @Override
    public Class<MonographPublicationDTO> dtoClass() {
        return MonographPublicationDTO.class;
    }

    @Override
    public void restore(Integer entityId, MonographPublicationDTO dto) {
        monographPublicationService.editMonographPublication(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return monographPublicationService.readMonographPublicationById(entityId);
    }
}
