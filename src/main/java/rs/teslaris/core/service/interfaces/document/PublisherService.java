package rs.teslaris.core.service.interfaces.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.model.document.Publisher;

@Service
public interface PublisherService {

    Page<PublisherDTO> readAllPublishers(Pageable pageable);

    Publisher findPublisherById(Integer publisherId);

    Publisher createPublisher(PublisherDTO publisherDTO);

    Publisher createPublisher(PublisherBasicAdditionDTO publisherDTO);

    void updatePublisher(PublisherDTO publisherDTO, Integer publisherId);

    void deletePublisher(Integer publisherId);
}
