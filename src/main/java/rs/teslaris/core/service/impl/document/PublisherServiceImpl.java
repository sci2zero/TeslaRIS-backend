package rs.teslaris.core.service.impl.document;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rs.teslaris.core.util.exceptionhandling.exception.NotFoundException;
import rs.teslaris.core.model.document.Publisher;
import rs.teslaris.core.repository.document.PublisherRepository;
import rs.teslaris.core.service.interfaces.document.PublisherService;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;


    @Override
    public Publisher findPublisherById(Integer publisherId) {
        return publisherRepository.findById(publisherId)
            .orElseThrow(() -> new NotFoundException("Publisher with given ID does not exist."));
    }
}
