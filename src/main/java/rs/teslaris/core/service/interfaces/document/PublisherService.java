package rs.teslaris.core.service.interfaces.document;

import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;

@Service
public interface PublisherService {

    Publisher findPublisherById(Integer publisherId);

    Publisher createPublisher(PublisherDTO publisherDTO);

    void updatePublisher(PublisherDTO publisherDTO, Integer publisherId);

    void deletePublisher(Integer publisherId);
}
