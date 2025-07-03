package rs.teslaris.core.service.interfaces.document;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import rs.teslaris.core.dto.document.PublisherBasicAdditionDTO;
import rs.teslaris.core.dto.document.PublisherDTO;
import rs.teslaris.core.indexmodel.PublisherIndex;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.service.interfaces.JPAService;

@Service
public interface PublisherService extends JPAService<Publisher> {

    Page<PublisherDTO> readAllPublishers(Pageable pageable);

    PublisherDTO readPublisherById(Integer publisherId);

    Page<PublisherIndex> searchPublishers(List<String> tokens, Pageable pageable);

    Publisher createPublisher(PublisherDTO publisherDTO, Boolean index);

    Publisher createPublisher(PublisherBasicAdditionDTO publisherDTO);

    void editPublisher(Integer publisherId, PublisherDTO publisherDTO);

    void deletePublisher(Integer publisherId);

    void forceDeletePublisher(Integer publisherId);

    CompletableFuture<Void> reindexPublishers();

    void indexPublisher(Publisher publisher);
}
