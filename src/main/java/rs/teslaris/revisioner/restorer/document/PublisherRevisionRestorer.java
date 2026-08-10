package rs.teslaris.revisioner.restorer.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.EntityType;
import rs.teslaris.core.service.interfaces.document.PublisherService;
import rs.teslaris.revisioner.restorer.RevisionRestorer;

@Component
@RequiredArgsConstructor
public class PublisherRevisionRestorer implements RevisionRestorer<PublisherDTO> {

    private final PublisherService publisherService;


    @Override
    public String entityType() {
        return EntityType.PUBLISHER.name();
    }

    @Override
    public Class<PublisherDTO> dtoClass() {
        return PublisherDTO.class;
    }

    @Override
    public void restore(Integer entityId, PublisherDTO dto) {
        publisherService.editPublisher(entityId, dto);
    }

    @Override
    public Object readCurrentState(Integer entityId) {
        return publisherService.readPublisherById(entityId);
    }
}
