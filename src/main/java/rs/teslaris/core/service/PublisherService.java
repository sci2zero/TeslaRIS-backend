package rs.teslaris.core.service;

import org.springframework.stereotype.Service;
import rs.teslaris.core.model.document.Publisher;

@Service
public interface PublisherService {

    Publisher findPublisherById(Integer publisherId);
}
